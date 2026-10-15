package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

public class TopLevelStringTest {

	private static final String LONG = "\"hello world, a long string\"";
	private static final String LONG_TRUNCATED = "\"hello... + 21\"";

	private static final int NO_MAX_STRING_LENGTH = Integer.MAX_VALUE;

	private static String process(String input, int offset, int length, int maxStringLength, boolean removeWhitespace) {
		StringBuilder builder = new StringBuilder();
		TopLevelString.process(input.toCharArray(), offset, length, builder, null, maxStringLength, removeWhitespace, CharArrayRangesFilter.DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, CharArrayRangesFilter.DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, CharArrayRangesFilter.DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
		String chars = builder.toString();

		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		TopLevelString.process(input.getBytes(StandardCharsets.UTF_8), offset, length, output, null, maxStringLength, removeWhitespace, ByteArrayRangesFilter.DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, ByteArrayRangesFilter.DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, ByteArrayRangesFilter.DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
		String bytes = output.toString(StandardCharsets.UTF_8);

		assertEquals(chars, bytes, input);

		return chars;
	}

	private static String process(String input, int maxStringLength, boolean removeWhitespace) {
		return process(input, 0, input.length(), maxStringLength, removeWhitespace);
	}

	@Test
	public void testIsTopLevelString() {
		for(String string : new String[] {"\"\"", "\"abc\"", " \t\r\n\"abc\" "}) {
			assertTrue(TopLevelString.isTopLevelString(string.toCharArray(), 0, string.length()), string);
			assertTrue(TopLevelString.isTopLevelString(string.getBytes(StandardCharsets.UTF_8), 0, string.length()), string);
		}
		for(String string : new String[] {"", "  ", "{\"a\":\"b\"}", "[\"a\"]", "123", "true", "null"}) {
			assertFalse(TopLevelString.isTopLevelString(string.toCharArray(), 0, string.length()), string);
			assertFalse(TopLevelString.isTopLevelString(string.getBytes(StandardCharsets.UTF_8), 0, string.length()), string);
		}
	}

	@Test
	public void testIsTopLevelStringForSlice() {
		String string = "{\"a\": \"b\"}";

		assertTrue(TopLevelString.isTopLevelString(string.toCharArray(), 5, 4)); //  "b"
		assertTrue(TopLevelString.isTopLevelString(string.getBytes(StandardCharsets.UTF_8), 5, 4));

		// the first non-whitespace character in the slice must be a quote
		assertFalse(TopLevelString.isTopLevelString(string.toCharArray(), 4, 5)); // : "b"
		assertFalse(TopLevelString.isTopLevelString(string.getBytes(StandardCharsets.UTF_8), 4, 5));

		// a quote after the slice does not count
		assertFalse(TopLevelString.isTopLevelString(string.toCharArray(), 5, 1));
		assertFalse(TopLevelString.isTopLevelString(string.getBytes(StandardCharsets.UTF_8), 5, 1));
	}

	@Test
	public void testIsTopLevelStringForInvalidOffset() {
		// left to the regular filters, which handle the exception
		assertFalse(TopLevelString.isTopLevelString(new char[] {'"', '"'}, -1, 2));
		assertFalse(TopLevelString.isTopLevelString(new byte[] {'"', '"'}, -1, 2));

		assertFalse(TopLevelString.isTopLevelString(new char[] {}, 1, 1));
		assertFalse(TopLevelString.isTopLevelString(new byte[] {}, 1, 1));
	}

	@Test
	public void testPassthrough() {
		for(String input : new String[] {"\"hello\"", " \"hello\" \n", "\"a\\\"b\\\\\"", LONG}) {
			assertEquals(input, process(input, NO_MAX_STRING_LENGTH, false));
			assertEquals(input, process(input, 100, false));
		}
	}

	@Test
	public void testRemoveWhitespace() {
		assertEquals("\"hello\"", process(" \"hello\" \n", NO_MAX_STRING_LENGTH, true));
		assertEquals("\"hello\"", process("\r\n\t\"hello\"", NO_MAX_STRING_LENGTH, true));
		assertEquals("\"a\\\"b\\\\\"", process(" \"a\\\"b\\\\\" ", NO_MAX_STRING_LENGTH, true));
	}

	@Test
	public void testMaxStringLength() {
		assertEquals(LONG_TRUNCATED, process(LONG, 5, false));
		assertEquals(" " + LONG_TRUNCATED + "\n", process(" " + LONG + "\n", 5, false));
		assertEquals(LONG_TRUNCATED, process(" " + LONG + "\n", 5, true));
	}

	@Test
	public void testMaxStringLengthMetrics() {
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		StringBuilder builder = new StringBuilder();
		TopLevelString.process(LONG.toCharArray(), 0, LONG.length(), builder, metrics, 5, false, CharArrayRangesFilter.DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, CharArrayRangesFilter.DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, CharArrayRangesFilter.DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
		assertEquals(LONG_TRUNCATED, builder.toString());
		assertEquals(1, metrics.getMaxStringLength());

		metrics = new DefaultJsonFilterMetrics();
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		TopLevelString.process(LONG.getBytes(StandardCharsets.UTF_8), 0, LONG.length(), output, metrics, 5, false, ByteArrayRangesFilter.DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, ByteArrayRangesFilter.DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, ByteArrayRangesFilter.DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
		assertEquals(LONG_TRUNCATED, output.toString(StandardCharsets.UTF_8));
		assertEquals(1, metrics.getMaxStringLength());
	}

	@Test
	public void testSlice() {
		// a colon after the slice must not make the string look like a key
		String input = "x" + LONG + ":y";
		assertEquals(LONG_TRUNCATED, process(input, 1, LONG.length(), 5, false));

		// whitespace is removed within the slice only
		input = "x " + LONG + " :y";
		assertEquals(LONG_TRUNCATED, process(input, 1, LONG.length() + 2, 5, true));
		assertEquals(" " + LONG + " ", process(input, 1, LONG.length() + 2, NO_MAX_STRING_LENGTH, false));
	}

	@Test
	public void testRanges() {
		// no ranges for a string which is kept as-is
		assertEquals(0, TopLevelString.ranges(LONG.toCharArray(), 0, LONG.length(), NO_MAX_STRING_LENGTH, false, new CharArrayRangesFilter(-1, LONG.length())).getFilterIndex());
		assertEquals(0, TopLevelString.ranges(LONG.getBytes(StandardCharsets.UTF_8), 0, LONG.length(), NO_MAX_STRING_LENGTH, false, new ByteArrayRangesFilter(-1, LONG.length())).getFilterIndex());

		// one range per whitespace run, one for truncation
		String input = " " + LONG + "\n";
		assertEquals(9, TopLevelString.ranges(input.toCharArray(), 0, input.length(), 5, true, new CharArrayRangesFilter(-1, input.length())).getFilterIndex());
		assertEquals(9, TopLevelString.ranges(input.getBytes(StandardCharsets.UTF_8), 0, input.length(), 5, true, new ByteArrayRangesFilter(-1, input.length())).getFilterIndex());
	}

}
