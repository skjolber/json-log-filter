package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CharArrayWhitespaceFilterTest {

	@Test
	public void testFindWhitespace() {
		// whitespace inside strings, also after escaped quotes, is not whitespace to remove
		assertFindWhitespace(-1, "{\"a b\":\"c \\\" d\",\"e\":[1,2]}");
		assertFindWhitespace(0, " {\"a\":1}");
		assertFindWhitespace(5, "{\"a\":\n1}");
		assertFindWhitespace(14, "{\"a\":\"b\\\\\",\"c\" :1}");
		assertFindWhitespace(7, "{\"a\":1}\t");
	}

	private static void assertFindWhitespace(int expected, String json) {
		char[] chars = json.toCharArray();
		assertEquals(expected == -1 ? chars.length : expected, CharArrayWhitespaceFilter.findWhitespace(chars, 0, chars.length), json);

		// from an offset
		char[] offsetChars = ("xx" + json).toCharArray();
		assertEquals(expected == -1 ? offsetChars.length : expected + 2, CharArrayWhitespaceFilter.findWhitespace(offsetChars, 2, offsetChars.length), json);
	}
}
