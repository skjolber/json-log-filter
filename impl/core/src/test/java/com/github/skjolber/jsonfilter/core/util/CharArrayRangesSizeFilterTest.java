package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CharArrayRangesSizeFilterTest {

	private static CharArrayRangesSizeFilter newFilter(int jsonLength) {
		return new CharArrayRangesSizeFilter(12, jsonLength,
			"\"[removed]\"".toCharArray(),
			"\"***\"".toCharArray(),
			"...".toCharArray());
	}

	// --- skipObjectOrArrayMaxSizeMaxStringLength ---

	@Test
	public void testSkipLongKeyDirectlyFollowedByColon() {
		// A field name whose character span exceeds maxStringLength but is directly followed by ':'
		// exercises the key-detection branch (lines 127-129 in skipObjectOrArrayMaxSizeMaxStringLength).
		String json = "{\"longlongkey\":\"value\"}";
		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter filter = newFilter(json.length());
		filter.setLevel(1);
		filter.setMark(0);
		int result = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, 0, json.length(), json.length(), 3);
		assertTrue(result >= 0);
	}

	@Test
	public void testSkipLongKeyWithWhitespaceBeforeColon() {
		// A field name whose span exceeds maxStringLength and is followed by whitespace then ':'
		// exercises the whitespace-scan key-detection branch (lines 144-150).
		String json = "{\"longlongkey\" :\"value\"}";
		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter filter = newFilter(json.length());
		filter.setLevel(1);
		filter.setMark(0);
		int result = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, 0, json.length(), json.length(), 3);
		assertTrue(result >= 0);
	}

	// --- replacementGrowth ---

	@Test
	public void testReplacementGrowth() {
		// set when prune or anonymize messages make the output larger than the input
		CharArrayRangesSizeFilter filter = newFilter(10);
		assertFalse(filter.isReplacementGrowth());

		filter.setReplacementGrowth(true);
		assertTrue(filter.isReplacementGrowth());

		filter.setReplacementGrowth(false);
		assertFalse(filter.isReplacementGrowth());
	}

	// --- anonymizeSubtree ---

	@Test
	public void testAnonymizeSubtreeWithLeadingWhitespace() {
		// Whitespace characters inside the scanned object hit the 'case ' ':' branch (line 256).
		String json = "{ \"key\":\"val\" }";
		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter filter = newFilter(json.length());
		filter.setLevel(0);
		filter.setMark(0);
		int result = filter.anonymizeSubtree(chars, 0, json.length());
		assertTrue(result > 0);
	}

	@Test
	public void testAnonymizeSubtreeStopsAtValueWhichDoesNotFit() {
		// The anonymize message does not fit: stop at the start of the value and lower the max size limit to it,
		// so that the caller cannot include (part of) the raw value.
		// A number, a string and a string followed by whitespace.
		String[] jsons = {"[1,2]", "[\"a\",\"b\"]", "[\"a\" ,\"b\"]"};

		// the first value does not fit
		for(String json : jsons) {
			CharArrayRangesSizeFilter filter = newFilter(json.length());
			filter.setLevel(0);
			filter.setMark(0);
			assertEquals(1, filter.anonymizeSubtree(json.toCharArray(), 0, 6), json);
			assertEquals(1, filter.getMaxSizeLimit(), json);
			assertEquals(1, filter.getMark(), json);
			assertEquals(1, filter.getLevel(), json);
		}

		// the first value is anonymized, the second does not fit
		int[] valueOffsets = {3, 5, 6};
		int[] marks = {2, 4, 5};
		for(int i = 0; i < jsons.length; i++) {
			CharArrayRangesSizeFilter filter = newFilter(jsons[i].length());
			filter.setLevel(0);
			filter.setMark(0);
			assertEquals(valueOffsets[i], filter.anonymizeSubtree(jsons[i].toCharArray(), 0, 10), jsons[i]);
			assertEquals(valueOffsets[i], filter.getMaxSizeLimit(), jsons[i]);
			assertEquals(marks[i], filter.getMark(), jsons[i]);
			assertEquals(1, filter.getLevel(), jsons[i]);
		}
	}

	@Test
	public void testAnonymizeSubtreeValueFollowedByWhitespaceThenComma() {
		// A quoted value whose closing quote is followed by whitespace before a comma
		// exercises the whitespace-scan value path (lines 301, 310-311, 316-338, 343).
		String json = "[\"val1\" ,\"val2\"]";
		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter filter = newFilter(json.length());
		filter.setLevel(0);
		filter.setMark(0);
		int result = filter.anonymizeSubtree(chars, 0, json.length());
		assertTrue(result > 0);
	}

	@Test
	public void testAnonymizeSubtreeKeyWithWhitespaceBeforeColon() {
		// A field name followed by whitespace then ':' inside an anonymized subtree
		// exercises lines 313-315 (whitespace-key detection in anonymizeSubtree) and line 343.
		String json = "{\"k\" :\"vvv\"}";
		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter filter = newFilter(json.length());
		filter.setLevel(0);
		filter.setMark(0);
		int result = filter.anonymizeSubtree(chars, 0, json.length());
		assertTrue(result > 0);
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
			char[] chars = json.toCharArray();
			int quoteIndex = json.length() - 2;
			for(int start = 3; start < quoteIndex; start++) {
				CharArrayRangesSizeFilter expected = newFilter(json.length());
				CharArrayRangesSizeFilter actual = newFilter(json.length());

				String message = json + " cut at " + start;
				assertEquals(expected.addMaxLength(chars, start, quoteIndex, quoteIndex - start), actual.addMaxLengthMaxSize(chars, start, quoteIndex, quoteIndex - start), message);
				assertEquals(expected.getRemovedLength(), actual.getRemovedLength(), message);
				assertEquals(output(expected, chars), output(actual, chars), message);
			}
		}
	}

	private static String output(CharArrayRangesSizeFilter filter, char[] chars) {
		StringBuilder buffer = new StringBuilder();
		filter.filter(chars, 0, chars.length, buffer);
		return buffer.toString();
	}
}
