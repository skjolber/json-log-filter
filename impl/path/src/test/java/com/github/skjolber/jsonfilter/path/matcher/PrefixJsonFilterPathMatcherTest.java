package com.github.skjolber.jsonfilter.path.matcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

public class PrefixJsonFilterPathMatcherTest {

	@Test
	public void testConstructor() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJsonFilter());
		
		assertNotNull(matcher.getFilter());
	}

	@Test
	public void testMatching() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJsonFilter());
		
		assertThat(matcher.matches("/abcdef")).isTrue();
		assertThat(matcher.matches("/cdef")).isFalse();
	}
}
