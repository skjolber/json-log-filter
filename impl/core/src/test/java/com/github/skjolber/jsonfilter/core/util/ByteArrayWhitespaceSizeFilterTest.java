package com.github.skjolber.jsonfilter.core.util;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class ByteArrayWhitespaceSizeFilterTest {
	
	@Test
	public void testDeepBrackets1() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('{', '}', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.skipObjectMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 1, builder, null);
		assertEquals(offset, 200);
	}
	
	@Test
	public void testDeepBrackets2() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('{', '}', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 200, builder, 1, null);
		assertEquals(offset, 200);
	}

	@Test
	public void testDeepBrackets3() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('[', ']', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 200, builder, 1, null);
		assertEquals(offset, 200);
	}

	@Test
	public void testSkipLongValueFollowedByWhitespace() {
		// the whitespace after a value is not written, but does not make room for the (truncated) value itself
		byte[] text = ("[\"" + "v".repeat(50) + "\"" + " ".repeat(60) + "]").getBytes(StandardCharsets.UTF_8);

		// does not fit: the value is removed, and the document cut at the mark
		ByteArrayWhitespaceSizeFilter filter = newFilterWithinArray(1, 1, 10);
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text, 1, text.length, builder, 5, null);
		assertEquals(1, offset);
		assertEquals("[", builder.toString(StandardCharsets.UTF_8));
		assertEquals(1, filter.getMaxSizeLimit());
		assertEquals(1, filter.getLevel());

		// fits: the whitespace is skipped too
		filter = newFilterWithinArray(1, 1, 20);
		builder = new ResizableByteArrayOutputStream(1024);
		offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text, 1, text.length, builder, 5, null);
		assertEquals(text.length, offset);
		assertEquals("[\"vvvvv... + 45\"", builder.toString(StandardCharsets.UTF_8));
		assertEquals(0, filter.getLevel());
	}

	@Test
	public void testAnonymizeScalarValueMustFit() {
		// the anonymize message is longer than the values
		String string = "{\"secret\":[1,2,3,42,5],\"other\":\"" + "o".repeat(40) + "\"}";
		byte[] text = string.getBytes(StandardCharsets.UTF_8);
		int start = string.indexOf('[') + 1;
		int end = string.indexOf(']') + 1;

		// room for the first value only
		ByteArrayWhitespaceSizeFilter filter = newFilterWithinArray(2, start, start + 5);
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.anonymizeObjectOrArrayMaxSize(text, start, text.length, builder, null);
		assertEquals(start + 2, offset);
		assertEquals("{\"secret\":[\"*\"", builder.toString(StandardCharsets.UTF_8));
		assertEquals(start + 2, filter.getMaxSizeLimit());

		// room for all values
		filter = newFilterWithinArray(2, start, start + 20);
		builder = new ResizableByteArrayOutputStream(1024);
		offset = filter.anonymizeObjectOrArrayMaxSize(text, start, text.length, builder, null);
		assertEquals(end, offset);
		assertEquals("{\"secret\":[\"*\",\"*\",\"*\",\"*\",\"*\"", builder.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testSkipObjectOrArrayMaxSizeKeepsLimitBeyondReadLimit() {
		// the limit is not capped at the end of the input: prune and anonymize messages later in the document
		// can be longer than the values they replace, and need the room
		String text = "[ 1 , 2 ]";
		ByteArrayWhitespaceSizeFilter filter = newFilterWithinArray(1, 1, 100);
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(128);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, text.length(), builder, 5, null);
		assertEquals(text.length(), offset);
		// the caller writes the rest, from the flush offset
		assertEquals("[1,2", builder.toString(StandardCharsets.UTF_8));
		assertEquals(0, filter.getLevel());
		// four spaces removed, and the closing bracket
		assertEquals(105, filter.getMaxSizeLimit());
	}

	@Test
	public void testAnonymizeObjectOrArrayMaxSizeKeepsLimitBeyondReadLimit() {
		// the limit is not capped at the end of the input, see testSkipObjectOrArrayMaxSizeKeepsLimitBeyondReadLimit
		String text = "[ 1 , 2 ]";
		ByteArrayWhitespaceSizeFilter filter = newFilterWithinArray(1, 1, 100);
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(128);
		int offset = filter.anonymizeObjectOrArrayMaxSize(text.getBytes(StandardCharsets.UTF_8), 1, text.length(), builder, null);
		assertEquals(text.length(), offset);
		assertEquals("[\"*\",\"*\"", builder.toString(StandardCharsets.UTF_8));
		assertEquals(0, filter.getLevel());
		// four spaces removed, two messages two characters longer than the values, and the closing bracket
		assertEquals(101, filter.getMaxSizeLimit());
	}

	private static ByteArrayWhitespaceSizeFilter newFilterWithinArray(int level, int mark, int maxSizeLimit) {
		// state right after the opening bracket of an array, with nothing written yet
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("\"[removed]\"".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8),
				"... + ".getBytes(StandardCharsets.UTF_8));
		filter.getSquareBrackets()[level - 1] = true;
		filter.setLevel(level);
		filter.setMark(mark);
		filter.setFlushOffset(0);
		filter.setWrittenMark(0);
		filter.setMaxSizeLimit(maxSizeLimit);
		return filter;
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
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\":\"x\"}";
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
	}
	
	
	@Test
	public void testLongKeyWhitespace() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" : \"x\"}";
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
	}

	@Test
	public void testAnonymizeObjectOrArrayMaxSizeStopsAtValueWhichDoesNotFit() {
		// The anonymize message does not fit: stop at the start of the value and lower the max size limit to it,
		// so that the caller cannot include part of the raw value.
		byte[] text = "[\"S,SN\",\"P]W\"]".getBytes(StandardCharsets.UTF_8);

		// the first value does not fit
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8),
				"truncateMessage".getBytes(StandardCharsets.UTF_8));
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setMaxSizeLimit(3);
		int offset = filter.anonymizeObjectOrArrayMaxSize(text, 1, text.length, builder, null);
		assertEquals(1, offset);
		assertEquals(1, filter.getMaxSizeLimit());
		assertEquals(1, filter.getMark());
		assertEquals("", builder.toString(StandardCharsets.UTF_8));

		// the first value is anonymized, the second does not fit
		filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		builder = new ResizableByteArrayOutputStream(1024);
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setMaxSizeLimit(6);
		offset = filter.anonymizeObjectOrArrayMaxSize(text, 1, text.length, builder, null);
		assertEquals(8, offset);
		assertEquals(8, filter.getMaxSizeLimit());
		assertEquals(7, filter.getMark());
		assertEquals("[\"*\"", builder.toString(StandardCharsets.UTF_8));
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
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8),
				"... + ".getBytes(StandardCharsets.UTF_8));

		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(1024);
		int offset = filter.skipObjectMaxStringLength(input.getBytes(StandardCharsets.UTF_8), 1, maxStringLength, output, null);
		assertEquals(input.length(), offset);
		assertEquals(expected, output.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testGetTruncateString() {
		byte[] truncate = "truncate".getBytes(StandardCharsets.UTF_8);
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter(
			"prune".getBytes(StandardCharsets.UTF_8),
			"anon".getBytes(StandardCharsets.UTF_8),
			truncate);
		assertSame(truncate, filter.getTruncateString());
	}

	@Test
	public void testBaseClassGetMessageLengths() {
		ByteArrayWhitespaceSizeFilter baseFilter = new ByteArrayWhitespaceSizeFilter(
			"prune".getBytes(StandardCharsets.UTF_8), "anon".getBytes(StandardCharsets.UTF_8), "trunc".getBytes(StandardCharsets.UTF_8));
		assertEquals(5, baseFilter.getPruneMessageLength());
		assertEquals(4, baseFilter.getAnonymizeMessageLength());
	}
}
