package com.github.skjolber.jsonfilter.path.matcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.DefaultJacksonJsonFilter;

public class PrefixJsonFilterPathMatcherTest {

	@Test
	public void testConstructor() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJsonFilter());
		
		assertNotNull(matcher.getFilter(false));
	}

	@Test
	public void testMatching() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJsonFilter());
		
		assertThat(matcher.matches("/abcdef")).isTrue();
		assertThat(matcher.matches("/cdef")).isFalse();
	}
}
