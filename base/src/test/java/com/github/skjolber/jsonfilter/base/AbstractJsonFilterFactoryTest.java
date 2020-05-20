package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterFactory;
import com.github.skjolber.jsonfilter.JsonFilterFactory.JsonFilterFactoryProperty;

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
		factory.setAnonymizeFilters("/abc");
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertTrue(factory.isSingleAnonymizeFilter());
		assertTrue(factory.isActivePathFilters());
	}

	@Test
	public void testPrune() {
		factory.setPruneFilters("/def");
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertTrue(factory.isSinglePruneFilter());
		assertTrue(factory.isActivePathFilters());
	}

	@Test
	public void testSupportedProperties() {
		for (JsonFilterFactoryProperty p : JsonFilterFactory.JsonFilterFactoryProperty.values()) {
			assertTrue(p.getPropertyName(), factory.isPropertySupported(p.getPropertyName()));
		}
		
		assertFalse(factory.isPropertySupported("abc"));
	}

	@Test
	public void testUsingProperties() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), "/abc");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), "//def");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), 123);
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), 13);

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
	}

	@Test
	public void testUsingPropertiesAlternativeValues() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList("/abc"));
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList("//def"));
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), "123");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), "13");

		assertThat(factory.getMaxStringLength()).isEqualTo(123);
		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(factory.getMaxPathMatches()).isEqualTo(13);
	}
	
	@Test
	public void testUsingPropertiesAlternativeValues2() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), new String[] {"/abc"});
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), new String[] {"//def"});

		assertThat(factory.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(factory.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}

	@Test
	public void testUsingPropertiesIllegalValues() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty("abc", new Object());
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), new Object());
		});
	}	
	
	@Test
	public void testAnyPrefix() {
		assertTrue(AbstractJsonFilterFactory.hasAnyPrefix(new String[] {"//a"}));
		assertFalse(AbstractJsonFilterFactory.hasAnyPrefix(new String[] {"/a"}));
	}
	
	@Test
	public void clearProperties() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList());
		assertFalse(factory.isActivePathFilters());
		assertFalse(factory.isSinglePruneFilter());

		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList());
		assertFalse(factory.isActivePathFilters());
		assertFalse(factory.isSingleAnonymizeFilter());

	}
}
