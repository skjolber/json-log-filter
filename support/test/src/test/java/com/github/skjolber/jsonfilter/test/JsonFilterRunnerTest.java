package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonFilterRunnerTest {

	@Test
	public void testReplace() {
		String filterSurrogates = JsonFilterRunner.filterSurrogates("abcdef...TRUNCATED BY 123");
		
		assertEquals(filterSurrogates, "abcdef...TRUNCATED BY XX");
		
	}
	
}
