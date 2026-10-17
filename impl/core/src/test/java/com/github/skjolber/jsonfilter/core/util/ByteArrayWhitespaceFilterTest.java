package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class ByteArrayWhitespaceFilterTest {

	@Test
	public void testSkipWhitespace() {
		// the skip checks four characters per iteration, so cover every run length modulo four,
		// with each kind of whitespace and different characters after the run
		String[] runs = new String[] {" ", "\n", "\t", "\r", "  ", "\n ", " \t ", "\r\n  ", "\n    ", "\n     ", " \t\r\n \t\r\n", "\n         "};
		String[] prefixes = new String[] {"", "x", "{\"a\":1,", "[1,2"};
		// not non-ASCII: like the loops it replaces, the skip takes negative bytes for whitespace, and valid JSON
		// has none outside strings
		String[] suffixes = new String[] {"\"", "{", "}", "]", "1", "t", "!"};

		for (String prefix : prefixes) {
			for (String run : runs) {
				for (String suffix : suffixes) {
					String json = prefix + run + suffix + " ";

					assertEquals(prefix.length() + run.length(), ByteArrayWhitespaceFilter.skipWhitespace(json.getBytes(StandardCharsets.UTF_8), prefix.length()), json);
				}
			}
		}
	}

	@Test
	public void testProcess() {
		// appends to what is already in the output, growing a small buffer, from an offset into the input
		String json = "xx{\n  \"a\" : \"b c\",\n  \"d\": [ 1, 2 ],\t\"e\":{ }\r\n}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

		for(int capacity : new int[] {1, 4, 64}) {
			ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(capacity);
			output.write("abc".getBytes(StandardCharsets.UTF_8), 0, 3);

			ByteArrayWhitespaceFilter.process(bytes, 2, bytes.length, output);

			assertEquals("abc{\"a\":\"b c\",\"d\":[1,2],\"e\":{}}", output.toString(StandardCharsets.UTF_8));
		}
	}


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
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		assertEquals(expected == -1 ? bytes.length : expected, ByteArrayWhitespaceFilter.findWhitespace(bytes, 0, bytes.length), json);

		// from an offset
		byte[] offsetBytes = ("xx" + json).getBytes(StandardCharsets.UTF_8);
		assertEquals(expected == -1 ? offsetBytes.length : expected + 2, ByteArrayWhitespaceFilter.findWhitespace(offsetBytes, 2, offsetBytes.length), json);
	}

}
