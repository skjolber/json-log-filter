package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterFactoryProperty;

public class AbstractJsonFilterFactoryTest {

	private AbstractJsonFilterFactory factory;

	@BeforeEach
	public void init() {
		factory = new AbstractJsonFilterFactory() {
			
			@Override
			public JsonFilter newJsonFilter() {
				return null;
			}
		};
	}

	@Test
	public void testMaxStringLength() {
		assertFalse(factory.isActiveMaxStringLength());
		factory.setMaxStringLength(123);
		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertTrue(factory.isActiveMaxStringLength());
	}
	
	@Test
	public void testMaxSize() {
		assertFalse(factory.isActiveMaxSize());
		factory.setMaxSize(123);
		assertThat(factory.getMaxSize()).isEqualTo(123);
		assertTrue(factory.isActiveMaxSize());
	}
	
	@Test
	public void testMaxPathMatches() {
		factory.setMaxPathMatches(321);
		assertThat(factory.getMaxPathMatches()).isEqualTo(321);
	}

	@Test
	public void testAnonymize() {
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setPrune(Collections.emptyList());
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setAnonymize(Collections.emptyList());
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setAnonymize("/abc");
		assertThat(factory.getAnonymize()).isEqualTo(Arrays.asList("/abc"));
		assertTrue(factory.isSingleAnonymizeFilter());
		assertTrue(factory.isActivePathFilters());
		
		factory.setAnonymize(); // does not crash
		factory.addAnonymize("/def");
		assertThat(factory.getAnonymize()).isEqualTo(Arrays.asList("/abc", "/def"));
	}

	@Test
	public void testPrune() {
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setAnonymize(Collections.emptyList());
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setPrune(Collections.emptyList());
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setPrune("/def");
		assertThat(factory.getPrune()).isEqualTo(Arrays.asList("/def"));
		assertTrue(factory.isSinglePruneFilter());
		assertTrue(factory.isActivePathFilters());
		
		factory.setPrune(); // does not crash
		factory.addPrune("/abc");
		assertThat(factory.getPrune()).isEqualTo(Arrays.asList("/def", "/abc"));
	}

	@Test
	public void testSupportedProperties() {
		for (JsonFilterFactoryProperty p : JsonFilterFactoryProperty.values()) {
			assertTrue(p.getPropertyName(), factory.isPropertySupported(p.getPropertyName()));
		}
		
		assertFalse(factory.isPropertySupported("abc"));
	}

	@Test
	public void testUsingProperties() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), "/abc");
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), "//def");
		factory.setProperty(JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), 123);
		factory.setProperty(JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), 13);
		factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), 1024);

		factory.setProperty(JsonFilterFactoryProperty.PRUNE_MESSAGE.getPropertyName(), "prune");
		factory.setProperty(JsonFilterFactoryProperty.ANON_MESSAGE.getPropertyName(), "anon");
		factory.setProperty(JsonFilterFactoryProperty.TRUNCATE_MESSAGE.getPropertyName(), "truncate");

		factory.setProperty(JsonFilterFactoryProperty.REMOVE_WHITESPACE.getPropertyName(), Boolean.TRUE);

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymize()).isEqualTo(Arrays.asList("/abc"));
		assertThat(factory.getPrune()).isEqualTo(Arrays.asList("//def"));
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
		assertThat(factory.getPruneJsonValue()).isEqualTo("\"prune\"");
		assertThat(factory.getAnonymizeJsonValue()).isEqualTo("\"anon\"");
		assertThat(factory.getTruncateJsonStringValue()).isEqualTo("truncate");
		assertThat(factory.isRemoveWhitespace()).isTrue();
		assertThat(factory.getMaxSize()).isEqualTo(1024);
	}

	
	@Test
	public void testUsingPropertiesAlternativeValues() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList("/abc"));
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList("//def"));
		factory.setProperty(JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), "123");
		factory.setProperty(JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), "13");
		factory.setProperty(JsonFilterFactoryProperty.REMOVE_WHITESPACE.getPropertyName(), "true");
		factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), "1024");

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymize()).isEqualTo(Arrays.asList("/abc"));
		assertThat(factory.getPrune()).isEqualTo(Arrays.asList("//def"));
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
		assertThat(factory.isRemoveWhitespace()).isTrue();
		assertThat(factory.getMaxSize()).isEqualTo(1024);
	}
	
	@Test
	public void testUsingPropertiesAlternativeValues2() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), new String[] {"/abc"});
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), new String[] {"//def"});

		assertThat(factory.getAnonymize()).isEqualTo(Arrays.asList("/abc"));
		assertThat(factory.getPrune()).isEqualTo(Arrays.asList("//def"));
	}

	@Test
	public void testUsingPropertiesIllegalValues() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty("abc", new Object());
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.ANON_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.PRUNE_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.TRUNCATE_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.REMOVE_WHITESPACE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), new Object());
		});
	}	

	@Test
	public void clearProperties() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList());
		assertFalse(factory.isActivePathFilters());
		assertFalse(factory.isSinglePruneFilter());

		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList());
		assertFalse(factory.isActivePathFilters());
		assertFalse(factory.isSingleAnonymizeFilter());

	}
}
