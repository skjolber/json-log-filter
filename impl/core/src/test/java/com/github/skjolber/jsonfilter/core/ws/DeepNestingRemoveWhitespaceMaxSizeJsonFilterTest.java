package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

/**
 * Documents nested deeper than the initial bracket array (32 levels).
 */

public class DeepNestingRemoveWhitespaceMaxSizeJsonFilterTest {

	private static String nested(int depth) {
		StringBuilder builder = new StringBuilder("{\"x\":");
		for(int i = 0; i < depth; i++) {
			builder.append(i % 2 == 0 ? "[" : "{\"k\":");
		}
		builder.append('1');
		for(int i = depth - 1; i >= 0; i--) {
			builder.append(i % 2 == 0 ? ']' : '}');
		}
		builder.append(",\"tail\":\"" + "t".repeat(50) + "\"}");
		return builder.toString();
	}

	private static void assertValidJson(String json, String name) {
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			throw new AssertionError(name + ": invalid JSON " + json, e);
		}
	}

	@Test
	public void validOutput() {
		for(int depth : new int[] {31, 32, 33, 60, 100}) {
			String input = nested(depth);
			for(int maxSize = 10; maxSize < input.length(); maxSize += 7) {
				for(String path : new String[] {"$.x", "$.z"}) {
					JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths(path).withRemoveWhitespace(true).withMaxSize(maxSize).build();
					String name = depth + " " + maxSize + " " + path;

					String chars = filter.process(input);
					assertNotNull(chars, name);
					assertValidJson(chars, name);

					byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
					assertNotNull(bytes, name);
					assertEquals(chars, new String(bytes, StandardCharsets.UTF_8), name);
				}
			}
		}
	}

	@Test
	public void sameResultAsKeepingWhitespaceWhenNotTruncated() {
		// compact input which fits, so removing whitespace must give the same result as keeping it
		for(int depth : new int[] {31, 32, 33, 60, 100}) {
			String input = nested(depth);
			for(String path : new String[] {"$.x", "$.z"}) {
				JsonFilter removeWhitespace = DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths(path).withRemoveWhitespace(true).withMaxSize(input.length() - 1).build();
				JsonFilter keepWhitespace = DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths(path).build();

				String name = depth + " " + path;
				String expected = keepWhitespace.process(input);
				if(expected.length() < input.length()) {
					assertEquals(expected, removeWhitespace.process(input), name);
					assertEquals(expected, new String(removeWhitespace.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), name);
				}
			}
		}
	}
}
