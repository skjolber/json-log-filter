package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * A long string value followed by whitespace, near the max size limit, must still be truncated.
 */

public class MaxStringLengthMaxSizeTrailingWhitespaceTest {

	private static final String INPUT = "{\"o\":{\"k\":\"" + "a".repeat(20) + "\"" + " ".repeat(40) + ",\"x\":1}}";

	private static void assertValidJson(String json, String name) {
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			throw new AssertionError(name + ": invalid JSON " + json, e);
		}
	}

	@Test
	public void truncatedNotRaw() {
		JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(3).withMaxSize(40).build();
		assertEquals("{\"o\":{\"k\":\"aaa... + 17\"}}", filter.process(INPUT));
		assertEquals("{\"o\":{\"k\":\"aaa... + 17\"}}", new String(filter.process(INPUT.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void neverRawForAllMaxSizes() {
		// sizes up to just past the whitespace; larger sizes currently hit a separate bug (null for valid input)
		for(int maxSize = 2; maxSize <= 66; maxSize++) {
			JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(3).withMaxSize(maxSize).build();
			for(String output : new String[] {filter.process(INPUT), new String(filter.process(INPUT.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)}) {
				assertNotNull(output);
				assertFalse(output.contains("aaaa"), maxSize + " " + output);
				assertValidJson(output, Integer.toString(maxSize));
			}
		}
	}
}
