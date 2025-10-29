package com.github.skjolber.jsonfilter.core.util;

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


}
