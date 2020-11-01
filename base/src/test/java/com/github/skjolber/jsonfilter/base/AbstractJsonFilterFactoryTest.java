package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterFactoryProperty;

import net.bytebuddy.ByteBuddy;

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
	public void testMaxPathMatches() {
		factory.setMaxPathMatches(321);
		assertThat(factory.getMaxPathMatches()).isEqualTo(321);
	}

	@Test
	public void testAnonymize() {
		factory.setPruneFilters((List<String>)null);
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setPruneFilters((String[])null);
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setPruneFilters(new String[0]);
		assertFalse(factory.isSingleAnonymizeFilter());
		
		factory.setPruneFilters(Collections.emptyList());
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setAnonymizeFilters(Collections.emptyList());
		assertFalse(factory.isSingleAnonymizeFilter());

		factory.setAnonymizeFilters("/abc");
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertTrue(factory.isSingleAnonymizeFilter());
		assertTrue(factory.isActivePathFilters());
	}

	@Test
	public void testPrune() {
		factory.setAnonymizeFilters((List<String>)null);
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setAnonymizeFilters((String[])null);
		assertFalse(factory.isSinglePruneFilter());

		factory.setAnonymizeFilters(new String[0]);
		assertFalse(factory.isSinglePruneFilter());

		factory.setAnonymizeFilters(Collections.emptyList());
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setPruneFilters(Collections.emptyList());
		assertFalse(factory.isSinglePruneFilter());
		
		factory.setPruneFilters("/def");
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertTrue(factory.isSinglePruneFilter());
		assertTrue(factory.isActivePathFilters());
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

		factory.setProperty(JsonFilterFactoryProperty.PRUNE_MESSAGE.getPropertyName(), "prune");
		factory.setProperty(JsonFilterFactoryProperty.ANON_MESSAGE.getPropertyName(), "anon");
		factory.setProperty(JsonFilterFactoryProperty.TRUNCATE_MESSAGE.getPropertyName(), "truncate");

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
		assertThat(factory.getPruneJsonValue()).isEqualTo("\"prune\"");
		assertThat(factory.getAnonymizeJsonValue()).isEqualTo("\"anon\"");
		assertThat(factory.getTruncateJsonStringValue()).isEqualTo("truncate");
	}

	
	@Test
	public void testUsingPropertiesAlternativeValues() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList("/abc"));
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList("//def"));
		factory.setProperty(JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), "123");
		factory.setProperty(JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), "13");

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
	}
	
	@Test
	public void testUsingPropertiesAlternativeValues2() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), new String[] {"/abc"});
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), new String[] {"//def"});

		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
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
