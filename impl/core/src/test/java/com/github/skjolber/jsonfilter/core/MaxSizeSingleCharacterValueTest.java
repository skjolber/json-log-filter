package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * A single character value right after an opening bracket is kept if it fits within max size.
 */

public class MaxSizeSingleCharacterValueTest {

	private static final List<IntFunction<JsonFilter>> FILTERS = List.of(
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(5).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizeKeys("z").withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.z").withMaxStringLength(5).withMaxSize(maxSize).build()
	);

	private static void assertFiltered(String expected, String input, int maxSize) {
		for(IntFunction<JsonFilter> factory : FILTERS) {
			JsonFilter filter = factory.apply(maxSize);
			String name = filter.getClass().getSimpleName();

			assertEquals(expected, filter.process(input), name);
			assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), name);
		}
	}

	@Test
	public void keepsSingleCharacterValue() {
		assertFiltered("{\"a\":[1]}", "{\"a\":[1,2]}", 9);
		assertFiltered("{\"a\":[1]}", "{\"a\":[1, 2]}", 9);
		assertFiltered("[1]", "[1,2,3]", 3);
		assertFiltered("[[1]]", "[[1],[2]]", 5);
	}

	@Test
	public void doesNotKeepValueWhichDoesNotFit() {
		assertFiltered("{\"a\":[]}", "{\"a\":[1,2]}", 8);
		assertFiltered("[]", "[1,2,3]", 2);
		assertFiltered("[1,2]", "[1,2,3]", 5);
	}
}
