package com.github.skjolber.jsonfilter.core;

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
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

/**
 * A document which fits within max size can still grow beyond it when prune or anonymize
 * messages are longer than the values they replace.
 */

public class MaxSizeReplacementGrowthTest {

	private static final String[] INPUTS = new String[] {
		"[{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0},{\"id\":0}]",
		"{\"id\":1,\"a\":{\"id\":true,\"b\":[1,2]},\"c\":{\"id\":null}}"
	};

	private static final List<IntFunction<JsonFilter>> FILTERS = List.of(
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withPruneKeys("id").withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withAnonymizeKeys("id").withAnonymizeMessage("ANONYMIZED").withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withPruneKeys("id").withMaxStringLength(100).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withPrunePaths("$.a.id", "$.c.id").withAnonymizeKeys("id").withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withPruneKeys("id").withRemoveWhitespace(true).withMaxSize(maxSize).build(),
		(maxSize) -> DefaultJsonLogFilterBuilder.createInstance().withPrunePaths("$.a.id", "$.c.id").withRemoveWhitespace(true).withMaxSize(maxSize).build()
	);

	private static void assertValidJson(String json, String name) {
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			while(parser.nextToken() != null);
		} catch(Exception e) {
			throw new AssertionError(name + ": invalid JSON " + json, e);
		}
	}

	@Test
	public void outputWithinMaxSize() {
		for(String input : INPUTS) {
			for(int maxSize = input.length() / 2; maxSize <= input.length(); maxSize++) {
				for(IntFunction<JsonFilter> factory : FILTERS) {
					JsonFilter filter = factory.apply(maxSize);
					String name = filter.getClass().getSimpleName() + " " + maxSize;

					String chars = filter.process(input);
					assertNotNull(chars, name);
					assertTrue(chars.length() <= maxSize, name + ": " + chars);
					assertValidJson(chars, name);

					byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
					assertNotNull(bytes, name);
					assertTrue(bytes.length <= maxSize, name + ": " + new String(bytes, StandardCharsets.UTF_8));
				}
			}
		}
	}

	@Test
	public void metricsCountedOnce() {
		String input = INPUTS[0];
		for(IntFunction<JsonFilter> factory : List.of(FILTERS.get(0), FILTERS.get(4))) {
			JsonFilter filter = factory.apply(input.length());
			DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
			String output = filter.process(input, metrics);

			int pruned = output.split("\\[removed\\]", -1).length - 1;
			assertEquals(pruned, metrics.getPrune(), filter.getClass().getSimpleName() + " " + output);
			assertEquals(input.length(), metrics.getInputSize(), filter.getClass().getSimpleName());
		}
	}
}
