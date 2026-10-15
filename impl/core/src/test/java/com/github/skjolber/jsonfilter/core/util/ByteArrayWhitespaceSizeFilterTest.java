package com.github.skjolber.jsonfilter.core.util;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class ByteArrayWhitespaceSizeFilterTest {
	
	@Test
	public void testDeepBrackets1() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('{', '}', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.skipObjectMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 1, builder, null);
		assertEquals(offset, 200);
	}
	
	@Test
	public void testDeepBrackets2() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('{', '}', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 200, builder, 1, null);
		assertEquals(offset, 200);
	}

	@Test
	public void testDeepBrackets3() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = createBracket('[', ']', 100);
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(200);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 200, builder, 1, null);
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
	public void testDeepBracketsMaxSizeKeepsGrownSquareBrackets() {
		ByteArrayWhitespaceSizeFilter filter = createDeepBracketsFilter(160);

		String text = createMixedBracket(100);

		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, text.length(), builder, 100, null);
		assertEquals(120, offset);
		assertDeepSquareBrackets(filter, 40);
	}

	@Test
	public void testDeepBracketsAnonymizeMaxSizeKeepsGrownSquareBrackets() {
		ByteArrayWhitespaceSizeFilter filter = createDeepBracketsFilter(160);

		String text = createMixedBracket(100);

		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		int offset = filter.anonymizeObjectOrArrayMaxSize(text.getBytes(StandardCharsets.UTF_8), 1, text.length(), builder, null);
		assertEquals(120, offset);
		assertDeepSquareBrackets(filter, 40);
	}

	private ByteArrayWhitespaceSizeFilter createDeepBracketsFilter(int maxSizeLimit) {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));

		// state as set by the caller, just after the first bracket
		filter.getSquareBrackets()[0] = true;
		filter.setLevel(1);
		filter.setMark(1);
		filter.setFlushOffset(0);
		filter.setWrittenMark(0);
		filter.setMaxSizeLimit(maxSizeLimit);
		return filter;
	}

	private void assertDeepSquareBrackets(ByteArrayWhitespaceSizeFilter filter, int level) {
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
	
	@Test
	public void testLongKey() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\":\"x\"}";
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
	}
	
	
	@Test
	public void testLongKeyWhitespace() {
		ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter("pruneMessage".getBytes(StandardCharsets.UTF_8), "anonymizeMessage".getBytes(StandardCharsets.UTF_8), "truncateMessage".getBytes(StandardCharsets.UTF_8));
		
		String text = "{\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" : \"x\"}";
		
		ResizableByteArrayOutputStream builder = new ResizableByteArrayOutputStream(1024);
		filter.setMaxSizeLimit(19);
		int offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(text.getBytes(StandardCharsets.UTF_8), 1, 20, builder, 1, null);
		assertEquals(offset, text.length() - 4);
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
