package com.github.skjolber.jsonfilter.path.matcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.jackson.DefaultJacksonJsonFilter;

public class PrefixJsonFilterPathMatcherTest {

	@Test
	public void testConstructor() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJacksonJsonFilter(), new DefaultJsonFilter(), new DefaultJsonFilter(), 1024);
		
		assertNotNull(matcher.getFilter(false, 512));
	}

	@Test
	public void testMatching() {
		PrefixJsonFilterPathMatcher matcher = new PrefixJsonFilterPathMatcher("/abc", new DefaultJacksonJsonFilter(), new DefaultJacksonJsonFilter(), new DefaultJsonFilter(), new DefaultJsonFilter(), 1024);
		
		assertThat(matcher.matches("/abcdef")).isTrue();
		assertThat(matcher.matches("/cdef")).isFalse();
	}
}
