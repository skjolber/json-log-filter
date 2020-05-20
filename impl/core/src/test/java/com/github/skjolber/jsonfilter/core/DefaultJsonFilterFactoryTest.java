package com.github.skjolber.jsonfilter.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

import static com.google.common.truth.Truth.*;

import java.util.Arrays;

public class DefaultJsonFilterFactoryTest {

	private DefaultJsonFilterFactory factory;

	@BeforeEach
	public void init() {
		factory = DefaultJsonFilterFactory.newInstance();
	}

	@Test
	public void testDefault() {
		DefaultJsonFilter filter = (DefaultJsonFilter)factory.newJsonFilter();
		assertThat(filter).isInstanceOf(DefaultJsonFilter.class);
	}

	@Test
	public void testNoMaxPathMatchesWithoutPaths() {
		factory.setMaxPathMatches(1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.newJsonFilter();
		});		
	}	

	@Test
	public void testMaxStringLength() {
		factory.setMaxStringLength(123);
		AbstractJsonFilter filter = (AbstractJsonFilter)factory.newJsonFilter();
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}

	@Test
	public void testAnonFullPath() {
		factory.setAnonymizeFilters("/abc");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
	}

	@Test
	public void testAnonAnyPath() {
		factory.setAnonymizeFilters("//abc");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
	}	

	@Test
	public void testAnonFullPathMaxLength() {
		factory.setAnonymizeFilters("/abc");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}

	@Test
	public void testAnonAnyPathMaxLength() {
		factory.setAnonymizeFilters("//abc");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}	

	@Test
	public void testAnons() {
		factory.setAnonymizeFilters("/abc", "/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}	

	@Test
	public void testAnonsMaxLength() {
		factory.setAnonymizeFilters("/abc", "/def");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}	

	@Test
	public void testPruneFullPath() {
		factory.setPruneFilters("/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
	}

	@Test
	public void testPruneAnyPath() {
		factory.setPruneFilters("//def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}

	@Test
	public void testPruneFullPathMaxLength() {
		factory.setPruneFilters("/def");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}

	@Test
	public void testPruneAnyPathMaxLength() {
		factory.setMaxStringLength(123);
		factory.setPruneFilters("//def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}

	@Test
	public void testPrunes() {
		factory.setPruneFilters("/abc", "/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}	

	@Test
	public void testPrunesMaxLength() {
		factory.setPruneFilters("/abc", "/def");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}

	@Test
	public void testAnonPrunes() {
		factory.setAnonymizeFilters("//def");
		factory.setPruneFilters("/abc", "//def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "//def"});
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//def"});
	}		

	@Test
	public void testAll() {
		factory.setMaxStringLength(123);
		factory.setAnonymizeFilters("/abc");
		factory.setPruneFilters("//def");
		factory.setMaxPathMatches(13);

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
	}

	@Test
	public void testUsingProperties() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), "/abc");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), "//def");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), 123);
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), 13);

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
	}

	@Test
	public void testUsingPropertiesAlternativeValues() {
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList("/abc"));
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList("//def"));
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), "123");
		factory.setProperty(JsonFilterFactory.JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), "13");

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
	}

	@Test
	public void testUsingPropertiesIllegalValues() {
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

}
