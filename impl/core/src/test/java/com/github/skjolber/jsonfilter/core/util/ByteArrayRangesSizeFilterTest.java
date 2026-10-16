package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

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

	// --- addMaxLengthMaxSize ---

	@Test
	public void testAddMaxLengthMaxSizeLikeAddMaxLength() {
		// a copy of addMaxLength with a fast path for the alignment: cut each value at every position and compare
		String[] values = {
			"abcdefghijklmnopqrstuvwxyz0123456789",
			"ab\\ncd\\\\ef\\\"gh\\u00e9ij\\\\u1234kl\\\\\\u00e9mn\\uD83D\\uDE00op\\bq\\fr",
			"ab\uD83D\uDE00cd\u00e6ef\u20acgh\uD83D\uDE00ij\u00f8kl\uD83D\uDE00mn"
		};
		for(String value : values) {
			String json = "[\"" + value + "\"]";
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			int quoteIndex = bytes.length - 2;
			for(int start = 3; start < quoteIndex; start++) {
				ByteArrayRangesSizeFilter expected = newFilter(bytes.length);
				ByteArrayRangesSizeFilter actual = newFilter(bytes.length);

				String message = json + " cut at " + start;
				assertEquals(expected.addMaxLength(bytes, start, quoteIndex, quoteIndex - start), actual.addMaxLengthMaxSize(bytes, start, quoteIndex, quoteIndex - start), message);
				assertEquals(expected.getRemovedLength(), actual.getRemovedLength(), message);
				assertEquals(output(expected, bytes), output(actual, bytes), message);
			}
		}
	}

	private static String output(ByteArrayRangesSizeFilter filter, byte[] bytes) {
		ResizableByteArrayOutputStream buffer = new ResizableByteArrayOutputStream(128);
		filter.filter(bytes, 0, bytes.length, buffer);
		return buffer.toString(StandardCharsets.UTF_8);
	}
}
