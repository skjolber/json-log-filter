package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonFilterDirectoryUnitTestCollectionRunnerTest {

	@Test
	public void testReplace() {
		String filterSurrogates = JsonNormalizer.filterMaxStringLength("abcdef... + 123");
		
		assertEquals(filterSurrogates, "abcdef... + XX");
	}
	
}
