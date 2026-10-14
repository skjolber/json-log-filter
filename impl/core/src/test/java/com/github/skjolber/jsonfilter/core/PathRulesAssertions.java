package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.UnaryOperator;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * Runs a path rule configuration through all filter variants, which must give the same result.
 */

public class PathRulesAssertions {

	private static List<UnaryOperator<DefaultJsonLogFilterBuilder>> variants(int maxSize) {
		return List.of(
			(b) -> b,
			(b) -> b.withMaxStringLength(100),
			(b) -> b.withRemoveWhitespace(true),
			(b) -> b.withMaxStringLength(100).withRemoveWhitespace(true),
			(b) -> b.withMaxSize(maxSize),
			(b) -> b.withMaxStringLength(100).withMaxSize(maxSize),
			(b) -> b.withRemoveWhitespace(true).withMaxSize(maxSize)
		);
	}

	public static void assertFiltered(String expected, String input, UnaryOperator<DefaultJsonLogFilterBuilder> configuration) {
		// max size below the input size, so that max size filtering is active
		for(UnaryOperator<DefaultJsonLogFilterBuilder> variant : variants(input.length() - 1)) {
			JsonFilter filter = variant.apply(configuration.apply(DefaultJsonLogFilterBuilder.createInstance())).build();
			String name = filter.toString();

			assertEquals(expected, filter.process(input), name);
			assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), name);
		}
	}
}
