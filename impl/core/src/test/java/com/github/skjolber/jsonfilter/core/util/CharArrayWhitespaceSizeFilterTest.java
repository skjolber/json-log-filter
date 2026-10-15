package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CharArrayWhitespaceSizeFilterTest {
	
	@Test
	public void testDeepBrackets1() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());
		
		String text = createBracket('{', '}', 100);
		
		StringBuilder builder = new StringBuilder();
		int offset = filter.skipObjectMaxStringLength(text.toCharArray(), 1, 1, builder, null);
		assertEquals(offset, 200);
	}
	
	@Test
	public void testDeepBrackets2() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());
		
		String text = createBracket('{', '}', 100);
		
		StringBuilder builder = new StringBuilder();
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.toCharArray(), 1, 200, builder, 1, null);
		assertEquals(offset, 200);
	}

	@Test
	public void testDeepBrackets3() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());
		
		String text = createBracket('[', ']', 100);
		
		StringBuilder builder = new StringBuilder();
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.toCharArray(), 1, 200, builder, 1, null);
		assertEquals(offset, 200);
	}

	public String createBracket(char start, char end, int count) {
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < count; i++) {
			builder.append(start);
		}
		for(int i = 0; i < count; i++) {
			builder.append(end);
		}
		
		return builder.toString();
	}
	
	@Test
	public void testLongKey() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\":\"x\"}";
		
		StringBuilder builder = new StringBuilder();
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.toCharArray(), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
	}

	@Test
	public void testLongKeyWhitespace() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" : \"x\"}";
		
		StringBuilder builder = new StringBuilder();
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.toCharArray(), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
	}

	@Test
	public void testAnonymizeObjectOrArrayMaxSizeStopsAtValueWhichDoesNotFit() {
		// The anonymize message does not fit: stop at the start of the value and lower the max size limit to it,
		// so that the caller cannot include part of the raw value.
		String text = "[\"S,SN\",\"P]W\"]";

		// the first value does not fit
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "\"*\"".toCharArray(), "truncateMessage".toCharArray());
		StringBuilder builder = new StringBuilder();
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setMaxSizeLimit(3);
		int offset = filter.anonymizeObjectOrArrayMaxSize(text.toCharArray(), 1, text.length(), builder, null);
		assertEquals(1, offset);
		assertEquals(1, filter.getMaxSizeLimit());
		assertEquals(1, filter.getMark());
		assertEquals("", builder.toString());

		// the first value is anonymized, the second does not fit
		filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "\"*\"".toCharArray(), "truncateMessage".toCharArray());
		builder = new StringBuilder();
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setMaxSizeLimit(6);
		offset = filter.anonymizeObjectOrArrayMaxSize(text.toCharArray(), 1, text.length(), builder, null);
		assertEquals(8, offset);
		assertEquals(8, filter.getMaxSizeLimit());
		assertEquals(7, filter.getMark());
		assertEquals("[\"*\"", builder.toString());
	}

	@Test
	public void testSkipObjectMaxStringLengthRemovesWhitespaceAfterValues() {
		assertSkipObjectMaxStringLength("{\"a\":{\"b\":\"x\",\"c\":\"y\"}}", "{\"a\":{\"b\":\"x\"\n,\"c\":\"y\"\n}\n}", 10);
	}

	@Test
	public void testSkipObjectMaxStringLengthRemovesWhitespaceAfterValuesAtMaxStringLength() {
		// 10 chars is within max string length, 11 chars is over it, but too short to be truncated
		assertSkipObjectMaxStringLength("{\"a\":\"abcdefghij\",\"b\":\"abcdefghijk\"}", "{\"a\":\"abcdefghij\"\n,\"b\":\"abcdefghijk\"\n}", 10);
	}

	@Test
	public void testSkipObjectMaxStringLengthRemovesWhitespaceAfterTruncatedValues() {
		assertSkipObjectMaxStringLength("{\"a\":\"x\",\"b\":\"abcdefghij... + 16\",\"c\":\"y\"}", "{\"a\" : \"x\" ,\n\t\"b\": \"abcdefghijklmnopqrstuvwxyz\"\r\n, \"c\":\"y\"  }", 10);
	}

	private void assertSkipObjectMaxStringLength(String expected, String input, int maxStringLength) {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "... + ".toCharArray());

		StringBuilder builder = new StringBuilder();
		int offset = filter.skipObjectMaxStringLength(input.toCharArray(), 1, maxStringLength, builder, null);
		assertEquals(input.length(), offset);
		assertEquals(expected, builder.toString());
	}

	@Test
	public void testGetMessageLengths() {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter(
			"prune".toCharArray(), "anon".toCharArray(), "trunc".toCharArray());
		assertEquals(5, filter.getPruneMessageLength());
		assertEquals(4, filter.getAnonymizeMessageLength());
	}

	@Test
	public void testBaseClassGetMessageLengths() {
		CharArrayWhitespaceFilter baseFilter = new CharArrayWhitespaceFilter(
			"prune".toCharArray(), "anon".toCharArray(), "trunc".toCharArray());
		assertEquals(5, baseFilter.getPruneMessageLength());
		assertEquals(4, baseFilter.getAnonymizeMessageLength());
	}
}
