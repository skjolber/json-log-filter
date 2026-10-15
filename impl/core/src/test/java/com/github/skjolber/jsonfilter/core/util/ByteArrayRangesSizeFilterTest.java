package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ByteArrayRangesSizeFilterTest {

	private static ByteArrayRangesSizeFilter newFilter(int jsonLength) {
		return new ByteArrayRangesSizeFilter(12, jsonLength,
			"\"[removed]\"".getBytes(StandardCharsets.UTF_8),
			"\"***\"".getBytes(StandardCharsets.UTF_8),
			"...".getBytes(StandardCharsets.UTF_8));
	}

	// --- anonymizeSubtree ---

	@Test
	public void testAnonymizeSubtreeStopsAtValueWhichDoesNotFit() {
		// The anonymize message does not fit: stop at the start of the value and lower the max size limit to it,
		// so that the caller cannot include (part of) the raw value.
		// A number, a string and a string followed by whitespace.
		String[] jsons = {"[1,2]", "[\"a\",\"b\"]", "[\"a\" ,\"b\"]"};

		// the first value does not fit
		for(String json : jsons) {
			ByteArrayRangesSizeFilter filter = newFilter(json.length());
			filter.setLevel(0);
			filter.setMark(0);
			assertEquals(1, filter.anonymizeSubtree(json.getBytes(StandardCharsets.UTF_8), 0, 6), json);
			assertEquals(1, filter.getMaxSizeLimit(), json);
			assertEquals(1, filter.getMark(), json);
			assertEquals(1, filter.getLevel(), json);
		}

		// the first value is anonymized, the second does not fit
		int[] valueOffsets = {3, 5, 6};
		int[] marks = {2, 4, 5};
		for(int i = 0; i < jsons.length; i++) {
			ByteArrayRangesSizeFilter filter = newFilter(jsons[i].length());
			filter.setLevel(0);
			filter.setMark(0);
			assertEquals(valueOffsets[i], filter.anonymizeSubtree(jsons[i].getBytes(StandardCharsets.UTF_8), 0, 10), jsons[i]);
			assertEquals(valueOffsets[i], filter.getMaxSizeLimit(), jsons[i]);
			assertEquals(marks[i], filter.getMark(), jsons[i]);
			assertEquals(1, filter.getLevel(), jsons[i]);
		}
	}
}
