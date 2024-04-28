package com.github.skjolber.jsonfilter.path.matcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.DefaultJacksonJsonFilter;

public class PrefixJsonFilterPathMatcherTest {

	private JsonFilter validatingFilter = new DefaultJacksonJsonFilter();
	private JsonFilter validatingMaxSizeFilter = new DefaultJacksonJsonFilter();
	private JsonFilter nonvalidatingFilter = new DefaultJsonFilter();
	private JsonFilter nonvalidatingMaxSizeFilter = new DefaultJsonFilter();
	
	@Test
	public void testConstructor() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, 1024);
		
		assertNotNull(matcher.getFilter(false, 512));
	}

	@Test
	public void testMatching() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc",  validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, 1024);
		
		assertThat(matcher.matches("/abcdef")).isTrue();
		assertThat(matcher.matches("/cdef")).isFalse();
		
		assertSame(matcher.getFilter(false, 2), nonvalidatingFilter);
		assertSame(matcher.getFilter(true, 2), validatingFilter);
		assertSame(matcher.getFilter(false, 2 * 1024), nonvalidatingMaxSizeFilter);
		assertSame(matcher.getFilter(true, 2 * 1024), validatingMaxSizeFilter);
	}
}
