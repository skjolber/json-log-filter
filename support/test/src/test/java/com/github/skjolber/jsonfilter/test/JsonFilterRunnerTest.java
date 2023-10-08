package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonFilterRunnerTest {

	@Test
	public void testReplace() {
		String filterSurrogates = JsonNormalizer.filterMaxStringLength("abcdef...TRUNCATED BY 123");
		
		assertEquals(filterSurrogates, "abcdef...TRUNCATED BY XX");
	}
	
}
