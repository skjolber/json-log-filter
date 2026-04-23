package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class MaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainMaxSizeJsonFilter extends MaxSizeJsonFilter {

		public MustContrainMaxSizeJsonFilter(int maxSize) {
			super(maxSize);
		}
		
		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};
	
	public MaxSizeJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxSizeJsonFilter(size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxSizeJsonFilter(size));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, false), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizeJsonFilter filter = new MaxSizeJsonFilter(string.length());
		
		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new MaxSizeJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustContrainMaxSizeJsonFilter(size);
		assertThat(maxSize, new DefaultJsonFilter()).hasMaxSize();
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// Build 35 levels of nested objects/arrays to trigger grow() in processMaxSize
		// grow() fires when bracketLevel >= squareBrackets.length (32)
		// Use a long string value so maxSize < json.length() while still covering all 35 '{'
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(i).append("\":");
		}
		deepJson.append("\"").append("x".repeat(500)).append("\""); // long string to ensure maxSize < json.length()
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();

		// maxSize covers all 35 '{' (at positions ~0-350) but truncates before the long string
		MustContrainMaxSizeJsonFilter filter = new MustContrainMaxSizeJsonFilter(400);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), output));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

}
