package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

/**
 * The Jackson filters record input and output sizes, and count anonymized and pruned values.
 */

public class JacksonFilterMetricsTest {

	private static final String INPUT = "{\"a\":\"secret\",\"b\":1,\"c\":{\"d\":true},\"e\":[1,2]}";

	private static DefaultJsonFilterMetrics process(JsonFilter filter, StringBuilder output) {
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(INPUT.toCharArray(), 0, INPUT.length(), output, metrics));
		return metrics;
	}

	@Test
	public void sizes() {
		for(JsonFilter filter : new JsonFilter[] {
				JacksonJsonLogFilterBuilder.createInstance().build(),
				JacksonJsonLogFilterBuilder.createInstance().withMaxStringLength(3).build(),
				JacksonJsonLogFilterBuilder.createInstance().withMaxSize(30).build(),
				JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a").build(),
				JacksonJsonLogFilterBuilder.createInstance().withAnonymizeKeys("a").build(),
				JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a").withMaxSize(30).build()}) {
			StringBuilder output = new StringBuilder();
			DefaultJsonFilterMetrics metrics = process(filter, output);
			String name = filter.getClass().getSimpleName();
			assertEquals(INPUT.length(), metrics.getInputSize(), name);
			assertEquals(output.length(), metrics.getOutputSize(), name + " " + output);
		}
	}

	@Test
	public void scalarMatchesAreCounted() {
		assertEquals(2, process(JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a", "$.b").build(), new StringBuilder()).getAnonymize());
		assertEquals(2, process(JacksonJsonLogFilterBuilder.createInstance().withPrunePaths("$.a", "$.b").build(), new StringBuilder()).getPrune());
		assertEquals(2, process(JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a", "$.b").withMaxSize(INPUT.length() - 1).build(), new StringBuilder()).getAnonymize());
		assertEquals(2, process(JacksonJsonLogFilterBuilder.createInstance().withPrunePaths("$.a", "$.b").withMaxSize(INPUT.length() - 1).build(), new StringBuilder()).getPrune());
	}
}
