package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
	public void testMarkToLimitIncludesSingleCharacterValueAtMark() {
		// the value right after an opening bracket starts at the mark
		assertMarkToLimit(2, "[1,2,3]", 2, 2, 1);
		assertMarkToLimit(7, "{\"a\":[1,2]}", 7, 7, 6);
		assertMarkToLimit(3, "[[1],[2]]", 3, 3, 2);
		// whitespace between the value and the cut
		assertMarkToLimit(2, "[1 ,2]", 3, 3, 1);
	}

	@Test
	public void testMarkToLimitExcludesValueAtMark() {
		// value beyond the max size limit
		assertMarkToLimit(-1, "[1,2,3]", 2, 1, 1);
		// value continues after the cut
		assertMarkToLimit(-1, "[12,3]", 2, 2, 1);
		// a comma mark never ends the output
		assertMarkToLimit(-1, "[1,]", 3, 4, 2);
	}

	@Test
	public void testSingleCharacterValueAfterOpeningBracket() {
		// kept if it fits
		assertMaxSize("{\"a\":[1]}", "{\"a\":[1,2]}", 9);
		assertMaxSize("[1]", "[1,2,3]", 3);
		assertMaxSize("[[1]]", "[[1],[2]]", 5);
		// dropped if it does not fit
		assertMaxSize("{\"a\":[]}", "{\"a\":[1,2]}", 8);
		assertMaxSize("[]", "[1,2,3]", 2);
	}

	private static void assertMarkToLimit(int expected, String json, int offset, int maxSizeLimit, int mark) {
		assertEquals(expected, MaxSizeJsonFilter.markToLimit(json.toCharArray(), offset, json.length(), maxSizeLimit, mark), json);
		assertEquals(expected, MaxSizeJsonFilter.markToLimit(json.getBytes(StandardCharsets.UTF_8), offset, json.length(), maxSizeLimit, mark), json);
	}

	private static void assertMaxSize(String expected, String json, int maxSize) {
		MaxSizeJsonFilter filter = new MaxSizeJsonFilter(maxSize);
		assertEquals(expected, filter.process(json), json);
		assertEquals(expected, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), json);
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
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// A long leaf value ensures the size limit is reached during content processing, not before opening all brackets.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMaxSizeJsonFilter filter = new MustContrainMaxSizeJsonFilter(400);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), output));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

}
