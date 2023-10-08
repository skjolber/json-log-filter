package com.github.skjolber.jsonfilter.path.matcher;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.DefaultJacksonJsonFilter;

public class AllJsonFilterPathMatcherTest {

	@Test
	public void testPassthrough() {
		DefaultJsonFilter filter = new DefaultJsonFilter();
		DefaultJacksonJsonFilter defaultJacksonJsonFilter = new DefaultJacksonJsonFilter();
		
		AllJsonFilterPathMatcher all = new AllJsonFilterPathMatcher(defaultJacksonJsonFilter, defaultJacksonJsonFilter, filter, filter, 1024);
		assertTrue(all.matches("abc"));
		assertSame(all.getFilter(false, 1024), filter);
	}
}
