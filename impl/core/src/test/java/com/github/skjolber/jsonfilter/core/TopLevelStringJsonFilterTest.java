package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * Documents which are a single string value.
 */

public class TopLevelStringJsonFilterTest {

	private static final String LONG = "\"hello world, a long string\"";
	private static final String LONG_TRUNCATED = "\"hello... + 21\"";

	private static Map<String, UnaryOperator<DefaultJsonLogFilterBuilder>> rules() {
		Map<String, UnaryOperator<DefaultJsonLogFilterBuilder>> rules = new LinkedHashMap<>();
		rules.put("none", (b) -> b);
		rules.put("key", (b) -> b.withAnonymizeKeys("a"));
		rules.put("path", (b) -> b.withAnonymizePaths("$.a"));
		rules.put("paths", (b) -> b.withAnonymizePaths("$.a", "$.b"));
		rules.put("key and path", (b) -> b.withAnonymizePaths("$.b").withPruneKeys("a"));
		return rules;
	}

	private static void assertFiltered(String expected, String input, UnaryOperator<DefaultJsonLogFilterBuilder> configuration) {
		for(Map.Entry<String, UnaryOperator<DefaultJsonLogFilterBuilder>> rule : rules().entrySet()) {
			JsonFilter filter = configuration.apply(rule.getValue().apply(DefaultJsonLogFilterBuilder.createInstance())).build();
			String name = rule.getKey() + " " + filter;

			assertEquals(expected, filter.process(input), name);
			assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), name);
		}
	}

	@Test
	public void passthrough() {
		for(String input : new String[] {"\"hello\"", " \"hello\" \n", "\"a\\\"b\\\\\"", LONG}) {
			assertFiltered(input, input, (b) -> b);
			assertFiltered(input, input, (b) -> b.withMaxStringLength(100));
			assertFiltered(input, input, (b) -> b.withMaxSize(1000));
			assertFiltered(input, input, (b) -> b.withMaxStringLength(100).withMaxSize(1000));
		}
	}

	@Test
	public void removeWhitespace() {
		assertFiltered("\"hello\"", " \"hello\" \n", (b) -> b.withRemoveWhitespace(true));
		assertFiltered("\"hello\"", " \"hello\" \n", (b) -> b.withRemoveWhitespace(true).withMaxStringLength(100));
		assertFiltered("\"hello\"", " \"hello\" \n", (b) -> b.withRemoveWhitespace(true).withMaxSize(1000));
	}

	@Test
	public void maxStringLength() {
		assertFiltered(LONG_TRUNCATED, LONG, (b) -> b.withMaxStringLength(5));
		assertFiltered(" " + LONG_TRUNCATED + "\n", " " + LONG + "\n", (b) -> b.withMaxStringLength(5));
		assertFiltered(LONG_TRUNCATED, " " + LONG + "\n", (b) -> b.withMaxStringLength(5).withRemoveWhitespace(true));
		assertFiltered(LONG_TRUNCATED, LONG, (b) -> b.withMaxStringLength(5).withMaxSize(1000));
	}

	@Test
	public void maxSizeSmallerThanString() {
		// a string cannot be cut, so it is kept as-is, or truncated by max string length
		assertFiltered(LONG, LONG, (b) -> b.withMaxSize(10));
		assertFiltered(LONG_TRUNCATED, LONG, (b) -> b.withMaxStringLength(5).withMaxSize(10));
	}

	@Test
	public void slice() {
		// a colon after the slice must not make the string look like a key
		String input = "x" + LONG + ":y";
		for(Map.Entry<String, UnaryOperator<DefaultJsonLogFilterBuilder>> rule : rules().entrySet()) {
			JsonFilter filter = rule.getValue().apply(DefaultJsonLogFilterBuilder.createInstance()).withMaxStringLength(5).build();
			String name = rule.getKey() + " " + filter;

			StringBuilder builder = new StringBuilder();
			assertTrue(filter.process(input.toCharArray(), 1, LONG.length(), builder), name);
			assertEquals(LONG_TRUNCATED, builder.toString(), name);

			ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
			assertTrue(filter.process(input.getBytes(StandardCharsets.UTF_8), 1, LONG.length(), output), name);
			assertEquals(LONG_TRUNCATED, output.toString(StandardCharsets.UTF_8), name);
		}
	}
}
