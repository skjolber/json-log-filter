package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.DefaultJsonLogFilterBuilder;

/**
 * Whitespace after a value is not written, but must not make room for the value itself.
 */

public class RemoveWhitespaceMaxSizeOvershootTest {

	private static final String LONG_VALUE_AND_WHITESPACE = "[\"" + "v".repeat(200) + "\"" + " ".repeat(300) + "]";
	private static final String NESTED_LONG_VALUE_AND_WHITESPACE = "{\"o\":{\"k\":\"" + "v".repeat(50) + "\"" + " ".repeat(60) + "},\"t\":1}";
	private static final String KEY_VALUE_AND_WHITESPACE = "{\"k\":\"abcdefgh\"" + " ".repeat(20) + "}";
	private static final String ANONYMIZED_SCALARS = "{\"secret\":[1,2,3,42,5],\"other\":\"" + "o".repeat(40) + "\"}";

	private static final List<IntFunction<JsonFilter>> FILTERS = List.of(
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.zz").withRemoveWhitespace(true).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.s").withMaxStringLength(30).withRemoveWhitespace(true).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(1).withRemoveWhitespace(true).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(5).withRemoveWhitespace(true).withMaxSize(maxSize).build()
	);

	private static void assertValidJson(String json, String name) {
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			throw new AssertionError(name + ": invalid JSON " + json, e);
		}
	}

	private static void assertFiltered(String expected, String input, JsonFilter filter) {
		assertEquals(expected, filter.process(input));
		assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void longValueFollowedByWhitespace() {
		assertFiltered("[]", LONG_VALUE_AND_WHITESPACE, FILTERS.get(0).apply(10));
		assertFiltered("{}", KEY_VALUE_AND_WHITESPACE, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(1, 7));
	}

	@Test
	public void outputWithinMaxSize() {
		for(String input : new String[] {LONG_VALUE_AND_WHITESPACE, NESTED_LONG_VALUE_AND_WHITESPACE, KEY_VALUE_AND_WHITESPACE, ANONYMIZED_SCALARS}) {
			for(int maxSize = 2; maxSize < input.length(); maxSize++) {
				for(IntFunction<JsonFilter> factory : FILTERS) {
					JsonFilter filter = factory.apply(maxSize);
					String name = filter.getClass().getSimpleName() + " " + maxSize + " " + input;

					String chars = filter.process(input);
					assertNotNull(chars, name);
					assertTrue(chars.length() <= maxSize, name + ": " + chars);
					assertValidJson(chars, name);

					byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
					assertNotNull(bytes, name);
					assertEquals(chars, new String(bytes, StandardCharsets.UTF_8), name);
				}
			}
		}
	}
}
