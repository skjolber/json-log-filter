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
}
