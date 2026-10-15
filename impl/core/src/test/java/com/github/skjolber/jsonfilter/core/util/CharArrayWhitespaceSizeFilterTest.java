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

	@Test
	public void testDeepBracketsMaxSizeKeepsGrownSquareBrackets() {
		CharArrayWhitespaceSizeFilter filter = createDeepBracketsFilter(160);

		String text = createMixedBracket(100);

		StringBuilder builder = new StringBuilder();
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.toCharArray(), 1, text.length(), builder, 100, null);
		assertEquals(120, offset);
		assertDeepSquareBrackets(filter, 40);
	}

	@Test
	public void testDeepBracketsAnonymizeMaxSizeKeepsGrownSquareBrackets() {
		CharArrayWhitespaceSizeFilter filter = createDeepBracketsFilter(160);

		String text = createMixedBracket(100);

		StringBuilder builder = new StringBuilder();
		int offset = filter.anonymizeObjectOrArrayMaxSize(text.toCharArray(), 1, text.length(), builder, null);
		assertEquals(120, offset);
		assertDeepSquareBrackets(filter, 40);
	}

	private CharArrayWhitespaceSizeFilter createDeepBracketsFilter(int maxSizeLimit) {
		CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter("pruneMessage".toCharArray(), "anonymizeMessage".toCharArray(), "truncateMessage".toCharArray());

		// state as set by the caller, just after the first bracket
		filter.getSquareBrackets()[0] = true;
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setWrittenMark(0);
		filter.setMaxSizeLimit(maxSizeLimit);
		return filter;
	}

	private void assertDeepSquareBrackets(CharArrayWhitespaceSizeFilter filter, int level) {
		// max size was reached deeper than the initial 32 levels,
		// so the caller must see the grown bracket array
		assertEquals(level, filter.getLevel());

		boolean[] squareBrackets = filter.getSquareBrackets();
		assertEquals(64, squareBrackets.length);
		for(int i = 0; i < level; i++) {
			assertEquals(i % 2 == 0, squareBrackets[i]);
		}
	}

	public String createMixedBracket(int count) {
		// alternating arrays and objects: [{"k":[{"k":[ .. ]}]}]
		StringBuilder builder = new StringBuilder();

		for(int i = 0; i < count; i++) {
			builder.append(i % 2 == 0 ? "[" : "{\"k\":");
		}
		builder.append('1');
		for(int i = count - 1; i >= 0; i--) {
			builder.append(i % 2 == 0 ? ']' : '}');
		}

		return builder.toString();
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
