package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

/**
 * Max string length applies to the values of keys which do not match any path, also when max size is active.
 */

public class PathMaxStringLengthMaxSizeRemoveWhitespaceValueTest {

	private static final String NESTED = "{\"a\":\"" + "x".repeat(30) + "\",\"b\":{\"c\":\"" + "y".repeat(30) + "\"},\"d\":1}";
	private static final String SINGLE = "{\"k\":\"" + "z".repeat(40) + "\"}";
	private static final String SINGLE_AND_NUMBER = "{\"k\":\"" + "z".repeat(40) + "\",\"z\":1}";

	private static JsonFilter filter(int maxSize) {
		return DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.x").withMaxStringLength(5).withMaxSize(maxSize).withRemoveWhitespace(true).build();
	}

	private static void assertFiltered(String expected, String input, int maxSize) {
		JsonFilter filter = filter(maxSize);
		assertEquals(expected, filter.process(input));
		assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void valuesAreTruncated() {
		assertFiltered("{\"a\":\"xxxxx... + 25\",\"b\":{\"c\":\"yyyyy... + 25\"},\"d\":1}", NESTED, 60);
		assertFiltered("{\"k\":\"zzzzz... + 35\"}", SINGLE, 30);
		assertFiltered("{\"k\":\"zzzzz... + 35\",\"z\":1}", SINGLE_AND_NUMBER, 30);
	}

	@Test
	public void valuesAreTruncatedForAllMaxSizes() {
		for(String input : new String[] {NESTED, SINGLE, SINGLE_AND_NUMBER}) {
			for(int maxSize = 2; maxSize < input.length(); maxSize++) {
				JsonFilter filter = filter(maxSize);
				for(String output : new String[] {filter.process(input), new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)}) {
					assertNotNull(output);
					for(String run : new String[] {"xxxxxx", "yyyyyy", "zzzzzz"}) {
						assertFalse(output.contains(run), maxSize + " " + output);
					}
				}
			}
		}
	}
}
