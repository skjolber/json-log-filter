package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.jackson.JsonNormalizer;

public class JsonNormalizerTest {

	@Test
	public void testNormalize() {
		String value = "abcd😂efghijklmnopqrst";
		
		String normalize = JsonNormalizer.normalize(value);
		assertEquals(normalize, "abcd\\uD83D\\uDE02efghijklmnopqrst");
	}
}
