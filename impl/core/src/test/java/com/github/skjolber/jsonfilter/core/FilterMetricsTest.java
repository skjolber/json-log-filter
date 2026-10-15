package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

/**
 * Input and output sizes match the actual input and output, and max size is only recorded when the document is cut.
 */

public class FilterMetricsTest {

	private static final String PRETTY = "{\n  \"a\" : \"bbbbbbbbbb\",\n  \"c\" : [ 1, 2, 3 ],\n  \"d\" : { \"e\" : true }\n}";
	private static final String COMPACT = "{\"a\":\"bbbbbbbbbb\",\"c\":[1,2,3],\"d\":{\"e\":true}}";

	private static DefaultJsonFilterMetrics process(JsonFilter filter, String input, StringBuilder output) {
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(input.toCharArray(), 0, input.length(), output, metrics));
		return metrics;
	}

	private static void assertSizes(JsonFilter filter, String input) {
		StringBuilder output = new StringBuilder();
		DefaultJsonFilterMetrics metrics = process(filter, input, output);
		String name = filter.getClass().getSimpleName();
		assertEquals(input.length(), metrics.getInputSize(), name);
		assertEquals(output.length(), metrics.getOutputSize(), name + " " + output);

		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		DefaultJsonFilterMetrics byteMetrics = new DefaultJsonFilterMetrics();
		byte[] byteOutput = filter.process(bytes, byteMetrics);
		assertEquals(bytes.length, byteMetrics.getInputSize(), name);
		assertEquals(byteOutput.length, byteMetrics.getOutputSize(), name);
	}

	@Test
	public void defaultFilter() {
		assertSizes(DefaultJsonLogFilterBuilder.createInstance().build(), COMPACT);
	}

	@Test
	public void prettyPrinter() {
		assertSizes(new PrettyPrintingJsonFilter(Indent.newBuilder().build()), COMPACT);
	}

	@Test
	public void outputIncludesClosingBrackets() {
		// max size cuts within the object, so closing brackets are added
		assertSizes(DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(5).withMaxSize(30).build(), COMPACT);
		assertSizes(DefaultJsonLogFilterBuilder.createInstance().withAnonymizeKeys("e").withMaxSize(30).build(), COMPACT);
		assertSizes(DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.d.e").withMaxStringLength(5).withMaxSize(30).build(), COMPACT);
	}

	@Test
	public void maxSizeOnlyWhenCut() {
		// the document fits once whitespace is removed
		for(JsonFilter filter : new JsonFilter[] {
				DefaultJsonLogFilterBuilder.createInstance().withRemoveWhitespace(true).withMaxSize(COMPACT.length()).build(),
				DefaultJsonLogFilterBuilder.createInstance().withMaxStringLength(50).withRemoveWhitespace(true).withMaxSize(COMPACT.length()).build(),
				DefaultJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.z").withRemoveWhitespace(true).withMaxSize(COMPACT.length()).build()}) {
			StringBuilder output = new StringBuilder();
			DefaultJsonFilterMetrics metrics = process(filter, PRETTY, output);
			assertEquals(COMPACT, output.toString(), filter.getClass().getSimpleName());
			assertEquals(0, metrics.getMaxSize(), filter.getClass().getSimpleName());
		}
		JsonFilter cut = DefaultJsonLogFilterBuilder.createInstance().withRemoveWhitespace(true).withMaxSize(20).build();
		DefaultJsonFilterMetrics metrics = process(cut, PRETTY, new StringBuilder());
		assertTrue(metrics.getMaxSize() > 0);
	}
}
