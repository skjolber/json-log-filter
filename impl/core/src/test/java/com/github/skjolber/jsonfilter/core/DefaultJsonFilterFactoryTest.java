package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterFactoryProperty;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

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
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractJsonFilter)factory.newJsonFilter();
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertTrue(filter.isRemovingWhitespace());
	}

	@Test
	public void testMaxSizeMaxStringLength() {
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractJsonFilter filter = (AbstractJsonFilter)factory.newJsonFilter();
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testAnonFullPath() {
		factory.setAnonymizeFilters("/abc");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertTrue(filter.isRemovingWhitespace());
	}

	@Test
	public void testAnonFullPathMaxSize() {
		factory.setAnonymizeFilters("/abc");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testMultipleAnonFullPath() {
		factory.setAnonymizeFilters("/abc", "/def");
		MultiFullPathJsonFilter filter = (MultiFullPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}
	
	@Test
	public void testMultipleAnonMixedPath() {
		factory.setAnonymizeFilters("/abc", "//def");
		MultiPathJsonFilter filter = (MultiPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "//def"});
	}

	@Test
	public void testAnonAnyPath() {
		factory.setAnonymizeFilters("//abc");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
	}	

	@Test
	public void testAnonAnyPathMaxSize() {
		factory.setAnonymizeFilters("//abc");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	
	
	@Test
	public void testAnonFullPathMaxStringLength() {
		factory.setAnonymizeFilters("/abc");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertTrue(filter.isRemovingWhitespace());
	}

	@Test
	public void testAnonFullPathMaxLengthMaxSize() {
		factory.setAnonymizeFilters("/abc");
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
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
	public void testAnonAnyPathMaxLengthMaxSize() {
		factory.setAnonymizeFilters("//abc");
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	

	@Test
	public void testAnons() {
		factory.setAnonymizeFilters("/abc", "/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}
	
	@Test
	public void testAnonsMaxSize() {
		factory.setAnonymizeFilters("/abc", "/def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
		assertTrue(filter.isRemovingWhitespace());
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
	public void testAnonsMaxLengthMaxSize() {
		factory.setAnonymizeFilters("/abc", "/def");
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	

	@Test
	public void testPruneFullPath() {
		factory.setPruneFilters("/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertTrue(filter.isRemovingWhitespace());
	}
	
	@Test
	public void testPruneFullPathMaxSize() {
		factory.setPruneFilters("/def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
		assertTrue(filter.isRemovingWhitespace());
	}

	@Test
	public void testPruneAnyPath() {
		factory.setPruneFilters("//def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}

	@Test
	public void testPruneAnyPathMaxSize() {
		factory.setPruneFilters("//def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testPruneFullPathMaxStringLength() {
		factory.setPruneFilters("/def");
		factory.setMaxStringLength(123);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		
		factory.setRemoveWhitespace(true);
		filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertTrue(filter.isRemovingWhitespace());
	}

	@Test
	public void testPruneFullPathMaxLengthMaxSize() {
		factory.setPruneFilters("/def");
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
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
	public void testPruneAnyPathMaxLengthMaxSize() {
		factory.setMaxStringLength(123);
		factory.setPruneFilters("//def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testPrunes() {
		factory.setPruneFilters("/abc", "/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}

	@Test
	public void testPrunesMaxSize() {
		factory.setPruneFilters("/abc", "/def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testMultiplePrunes() {
		factory.setPruneFilters("/abc", "/def");
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}

	@Test
	public void testMultiplePrunesMaxSize() {
		factory.setPruneFilters("/abc", "/def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}
	
	@Test
	public void testMultiplePruneMixedPath() {
		factory.setPruneFilters("/abc", "//def");
		MultiPathJsonFilter filter = (MultiPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "//def"});
	}

	@Test
	public void testMultiplePruneMixedPathMaxSize() {
		factory.setPruneFilters("/abc", "//def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "//def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
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
	public void testPrunesMaxLengthMaxSize() {
		factory.setPruneFilters("/abc", "/def");
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
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
	public void testAnonPrunesMaxSize() {
		factory.setAnonymizeFilters("//def");
		factory.setPruneFilters("/abc", "//def");
		factory.setMaxSize(1024);
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "//def"});
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}		

	@Test
	public void testAll() {
		factory.setMaxStringLength(123);
		factory.setAnonymizeFilters("/abc");
		factory.setPruneFilters("//def");
		factory.setMaxPathMatches(13);
		factory.setPruneStringValue("prune");
		factory.setAnonymizeStringValue("anon");
		factory.setTruncateStringValue("truncate");
		factory.setMaxSize(1024);
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
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
		factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), 1024);

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	@Test
	public void testUsingPropertiesAlternativeValues() {
		factory.setProperty(JsonFilterFactoryProperty.ANONYMIZE.getPropertyName(), Arrays.asList("/abc"));
		factory.setProperty(JsonFilterFactoryProperty.PRUNE.getPropertyName(), Arrays.asList("//def"));
		factory.setProperty(JsonFilterFactoryProperty.MAX_STRING_LENGTH.getPropertyName(), "123");
		factory.setProperty(JsonFilterFactoryProperty.MAX_PATH_MATCHES.getPropertyName(), "13");
		factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), "1024");

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();

		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxPathMatches()).isEqualTo(13);
	}

	@Test
	public void testUsingPropertiesIllegalValues() {
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
			factory.setProperty(JsonFilterFactoryProperty.PRUNE_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.ANON_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.TRUNCATE_MESSAGE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty(JsonFilterFactoryProperty.MAX_SIZE.getPropertyName(), new Object());
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			factory.setProperty("", new Object());
		});
	}	

}
