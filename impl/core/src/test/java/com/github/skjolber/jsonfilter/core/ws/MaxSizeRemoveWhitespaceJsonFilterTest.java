package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class MaxSizeRemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainMaxSizeRemoveWhitespaceJsonFilter extends MaxSizeRemoveWhitespaceJsonFilter {
		public MustContrainMaxSizeRemoveWhitespaceJsonFilter(int maxSize) {
			super(maxSize);
		}
		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};

	public MaxSizeRemoveWhitespaceJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizeRemoveWhitespaceJsonFilter filter = new MaxSizeRemoveWhitespaceJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new MaxSizeRemoveWhitespaceJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeRemoveWhitespaceJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeRemoveWhitespaceJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeRemoveWhitespaceJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size);
		assertThat(maxSize, new RemoveWhitespaceJsonFilter()).hasMaxSize().hasMaxSizeMetrics();
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// A long leaf value ensures the size limit is reached during content processing, not before opening all brackets.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMaxSizeRemoveWhitespaceJsonFilter(400);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), output));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testWithMetrics() throws Exception {
		MustContrainMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMaxSizeRemoveWhitespaceJsonFilter(1000);
		com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics metrics = new com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics();
		String json = "{ \"key\" : \"value\" }";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb, metrics));

		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		metrics = new com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut, metrics));
	}

	@Test
	public void testMetricsExceptionReturnsFalse() throws Exception {
		MustContrainMaxSizeRemoveWhitespaceJsonFilter filter = new MustContrainMaxSizeRemoveWhitespaceJsonFilter(100);
		com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics metrics = new com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics();
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}


}
