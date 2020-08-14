package com.github.skjolber.jsonfilter.spring;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.spring.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.spring.matcher.JsonFilterPathMatcher;

public class JsonFilterAutoConfigurationTest {

	@Test 
	public void testAllForEmptyMatcher() {
		
		JsonFilter filter = new DefaultJsonFilter();
		
		JsonFilterPathMatcher matcher = new JsonFilterAutoConfiguration().toFilter(null, null, filter);
		assertTrue(matcher instanceof AllJsonFilterPathMatcher);
	}
}
