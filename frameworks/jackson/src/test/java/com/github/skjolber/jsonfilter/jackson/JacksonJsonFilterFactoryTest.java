package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class JacksonJsonFilterFactoryTest {

	private JacksonJsonFilterFactory factory;

	@BeforeEach
	public void init() {
		factory = JacksonJsonFilterFactory.newInstance();
	}

	@Test
	public void testDefault() {
		assertThat(factory.newJsonFilter()).isInstanceOf(DefaultJsonFilter.class);
	}

	@Test
	public void testMaxSize() {
		factory.setMaxSize(1024);
		
		AbstractJsonFilter filter = (AbstractJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}
	
	@Test
	public void testMaxStringLength() {
		factory.setMaxStringLength(123);
		
		AbstractJsonFilter filter = (AbstractJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
	}
	

	@Test
	public void testMaxStringLengthMaxSize() {
		factory.setMaxStringLength(123);
		factory.setMaxSize(1024);
		
		AbstractJsonFilter filter = (AbstractJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}

	
	@Test
	public void testAnon1() {
		factory.setAnonymizeFilters("/abc");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
	}
	
	@Test
	public void testAnon2() {
		factory.setAnonymizeFilters("//abc");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
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
	}	
	
	@Test
	public void testPrune1() {
		factory.setMaxStringLength(123);
		factory.setPruneFilters("//def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}

	@Test
	public void testPrune2() {
		factory.setMaxStringLength(123);
		factory.setPruneFilters("//def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}
	
	@Test
	public void testPrunes() {
		factory.setPruneFilters("/abc", "/def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}	

	@Test
	public void testPrunesMaxSize() {
		factory.setMaxSize(1024);
		factory.setPruneFilters("/abc", "/def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	

	@Test
	public void testAll() {
		factory.setMaxStringLength(123);
		factory.setAnonymizeFilters("/abc");
		factory.setPruneFilters("//def");
		factory.setMaxSize(1024);

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}
	
}
