package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class JacksonJsonFilterFactoryTest {

	private JacksonJsonFilterFactory factory;

	@BeforeEach
	public void init() {
		factory = JacksonJsonFilterFactory.newInstance();
	}

	@Test
	public void testDefault() {
		// validates, then passes the document through as-is
		JsonFilter filter = factory.newJsonFilter();
		assertThat(filter).isInstanceOf(DefaultJacksonJsonFilter.class);
		assertThat(filter.isValidating()).isTrue();
		assertThat(filter.isRemovingWhitespace()).isFalse();
		assertThat(filter.process("{\"a\":1}, \"b\":2}")).isNull();
	}

	@Test
	public void testRemoveWhitespace() {
		factory.setRemoveWhitespace(true);

		JsonFilter filter = factory.newJsonFilter();
		assertThat(filter.isRemovingWhitespace()).isTrue();
		assertThat(filter.process("{\n  \"a\" : [ 1, 2 ]\n}")).isEqualTo("{\"a\":[1,2]}");
	}

	@Test
	public void testRemoveWhitespaceMaxSize() {
		factory.setRemoveWhitespace(true);
		factory.setMaxSize(1024);

		// small documents must not be passed through as-is
		JsonFilter filter = factory.newJsonFilter();
		assertThat(filter.isRemovingWhitespace()).isTrue();
		assertThat(filter.process("{\n  \"a\" : [ 1, 2 ]\n}")).isEqualTo("{\"a\":[1,2]}");
	}

	@Test
	public void testMaxSizeDoesNotClaimToRemoveWhitespace() {
		factory.setMaxSize(1024);

		// small documents are passed through as-is
		assertThat(factory.newJsonFilter().isRemovingWhitespace()).isFalse();
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
		factory.setAnonymize("/abc");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
	}
	
	@Test
	public void testAnon2() {
		factory.setAnonymize("//abc");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"//abc"});
	}	
	
	@Test
	public void testAnons() {
		factory.setAnonymize("/abc", "/def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}	
	
	@Test
	public void testAnonsMaxSize() {
		factory.setAnonymize("/abc", "/def");
		factory.setMaxSize(1024);
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	
	
	@Test
	public void testPrune1() {
		factory.setMaxStringLength(123);
		factory.setPrune("//def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}

	@Test
	public void testPrune2() {
		factory.setMaxStringLength(123);
		factory.setPrune("//def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
	}
	
	@Test
	public void testPrunes() {
		factory.setPrune("/abc", "/def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
	}	

	@Test
	public void testPrunesMaxSize() {
		factory.setMaxSize(1024);
		factory.setPrune("/abc", "/def");
		
		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"/abc", "/def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}	

	@Test
	public void testAll() {
		factory.setMaxStringLength(123);
		factory.setAnonymize("/abc");
		factory.setPrune("//def");
		factory.setMaxSize(1024);

		AbstractPathJsonFilter filter = (AbstractPathJsonFilter)factory.newJsonFilter();
		
		assertThat(filter.getMaxStringLength()).isEqualTo(123);
		assertThat(filter.getAnonymizeFilters()).isEqualTo(new String[]{"/abc"});
		assertThat(filter.getPruneFilters()).isEqualTo(new String[]{"//def"});
		assertThat(filter.getMaxSize()).isEqualTo(1024);
	}
	
}
