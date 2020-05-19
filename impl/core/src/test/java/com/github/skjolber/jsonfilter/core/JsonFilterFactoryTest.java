package com.github.skjolber.jsonfilter.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractRangesJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

import static com.google.common.truth.Truth.*;

public class JsonFilterFactoryTest {

	private JsonFilterFactory factory;

	@BeforeEach
	public void init() {
		factory = JsonFilterFactory.newInstance();
	}

	@Test
	public void testDefault() {
		DefaultJsonFilter filter = (DefaultJsonFilter)factory.newJsonFilter();
		assertThat(filter).isInstanceOf(DefaultJsonFilter.class);
	}

	@Test
	public void testMaxStringLength() {
		factory.setMaxStringLength(123);
		AbstractRangesJsonFilter filter = (AbstractRangesJsonFilter)factory.newJsonFilter();
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
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}
	
}
