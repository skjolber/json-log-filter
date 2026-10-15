package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * The Jackson max-size filters count separators, colons, opening brackets, escapes and UTF-8 bytes,
 * so that the output does not exceed the max size.
 */

public class JacksonMaxSizeAccuracyTest {

	private static final List<IntFunction<JsonFilter>> FILTERS = List.of(
		(maxSize) -> new JacksonMaxSizeJsonFilter(maxSize),
		(maxSize) -> new JacksonMaxSizeMaxStringLengthJsonFilter(5, maxSize),
		(maxSize) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.b").withPrunePaths("$.c").withMaxSize(maxSize).build()
	);

	private static final String[] INPUTS = new String[] {
		"{\"a\":1,\"b\":2,\"c\":3}",
		"{\"a\":{\"b\":{\"c\":1}},\"d\":2}",
		"{\"a\":1,\"b\":\"secretvalue\",\"c\":3}",
		"{\"k\":\"\\t\\t\\t\\t\\t\\t\\t\\t\",\"z\":1}",
		"{\"a\\\"\\\"\\\"\\\"\":1,\"b\":2}",
		"{\"a\":\"ææææææææææ\",\"b\":1}",
		"[1,[2,3],{\"x\":\"y\"},[{\"z\":[4,5]}]]",
		"{\"b\":{\"x\":1,\"y\":[1,2]},\"c\":{\"x\":1},\"d\":\"€€€\"}"
	};

	@Test
	public void outputWithinMaxSize() {
		// below the input size, where max size filtering is active; growth from replacement messages in documents
		// which fit is handled separately
		for(String input : INPUTS) {
			for(int maxSize = 2; maxSize < input.length(); maxSize++) {
				for(IntFunction<JsonFilter> factory : FILTERS) {
					JsonFilter filter = factory.apply(maxSize);
					String name = filter.getClass().getSimpleName() + " " + maxSize + " " + input;

					String chars = filter.process(input);
					assertNotNull(chars, name);
					assertTrue(chars.length() <= maxSize, name + ": " + chars);
				}
			}
			byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
			for(int maxSize = 2; maxSize < bytes.length; maxSize++) {
				for(IntFunction<JsonFilter> factory : FILTERS) {
					JsonFilter filter = factory.apply(maxSize);
					String name = filter.getClass().getSimpleName() + " " + maxSize + " " + input;

					byte[] output = filter.process(bytes);
					assertNotNull(output, name);
					assertTrue(output.length <= maxSize, name + ": " + new String(output, StandardCharsets.UTF_8));
				}
			}
		}
	}

	@Test
	public void colonAfterFieldName() {
		assertEquals("{\"a\":1}", new JacksonMaxSizeJsonFilter(12).process(INPUTS[0]));
	}

	@Test
	public void openingBracket() {
		assertEquals("{}", new JacksonMaxSizeJsonFilter(6).process(INPUTS[1]));
	}

	@Test
	public void separatorBeforeSecondField() {
		JsonFilter filter = JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.b").withMaxSize(14).build();
		assertEquals("{\"a\":1}", filter.process(INPUTS[2]));
	}

	@Test
	public void escapedValueAndFieldName() {
		assertEquals("{}", new JacksonMaxSizeJsonFilter(16).process(INPUTS[3]));
		assertEquals("{}", new JacksonMaxSizeJsonFilter(11).process(INPUTS[4]));
	}

	@Test
	public void utf8Bytes() {
		byte[] output = new JacksonMaxSizeJsonFilter(18).process(INPUTS[5].getBytes(StandardCharsets.UTF_8));
		assertEquals("{}", new String(output, StandardCharsets.UTF_8));
	}
}
