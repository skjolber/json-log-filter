package com.github.skjolber.jsonfilter.spring.matcher;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class AllJsonFilterPathMatcherTest {

	@Test
	public void testPassthrough() {
		DefaultJsonFilter filter = new DefaultJsonFilter();
		
		AllJsonFilterPathMatcher all = new AllJsonFilterPathMatcher(filter);
		assertTrue(all.matches("abc"));
		assertSame(all.getFilter(), filter);
	}
}
