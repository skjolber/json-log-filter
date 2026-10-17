package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
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
	public void testProcessStringReturnsInputWhenDocumentFits() {
		String json = "{\"key\":\"value\",\"list\":[1,2,3]}";

		MaxSizeJsonFilter filter = new MaxSizeJsonFilter(json.length());
		assertSame(json, filter.process(json));

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertSame(json, filter.process(json, metrics));
		assertEquals(json.length(), metrics.getInputSize());
		assertEquals(json.length(), metrics.getOutputSize());

		// one character too long: filtered as before
		MaxSizeJsonFilter smaller = new MaxSizeJsonFilter(json.length() - 1);
		assertEquals(smaller.process(json.toCharArray()), smaller.process(json));
		assertEquals(smaller.process(json.toCharArray(), new DefaultJsonFilterMetrics()), smaller.process(json, new DefaultJsonFilterMetrics()));
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
	public void testHasContent() {
		// whitespace and closing brackets are not content
		String json = "{\"a\":[1 \n\t\r ]}";
		char[] chars = json.toCharArray();
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

		assertTrue(MaxSizeJsonFilter.hasContent(chars, 0, chars.length));
		assertTrue(MaxSizeJsonFilter.hasContent(bytes, 0, bytes.length));

		assertTrue(MaxSizeJsonFilter.hasContent(chars, 6, chars.length)); // the 1
		assertTrue(MaxSizeJsonFilter.hasContent(bytes, 6, bytes.length));

		assertFalse(MaxSizeJsonFilter.hasContent(chars, 7, chars.length));
		assertFalse(MaxSizeJsonFilter.hasContent(bytes, 7, bytes.length));

		// only before the limit
		assertFalse(MaxSizeJsonFilter.hasContent(" }1".toCharArray(), 0, 2));
		assertFalse(MaxSizeJsonFilter.hasContent(" }1".getBytes(StandardCharsets.UTF_8), 0, 2));

		// a comma, and non-ASCII
		assertTrue(MaxSizeJsonFilter.hasContent(" ,".toCharArray(), 0, 2));
		assertTrue(MaxSizeJsonFilter.hasContent(" ,".getBytes(StandardCharsets.UTF_8), 0, 2));
		assertTrue(MaxSizeJsonFilter.hasContent(" \u00e6".toCharArray(), 0, 2));
		assertTrue(MaxSizeJsonFilter.hasContent(" \u00e6".getBytes(StandardCharsets.UTF_8), 0, 3));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void testNonStructuralIsKept() {
		// there is no smaller valid document
		MaxSizeJsonFilter filter = new MaxSizeJsonFilter(2);
		for(String json : new String[] {"\"abc\"", "-1.5e3", "123456", "true", "false", "null"}) {
			assertEquals(json, filter.process(json));
			assertEquals(json, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_leaves_output_untouched() throws Exception {
		// the output is written in one go at the end, so invalid input fails before anything is written:
		// fail in the metrics callback instead, after the output is written
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics() {
			@Override
			public void onInput(int inputSize) {
				throw new IllegalStateException();
			}
		};
		String string = "{\"a\":\"b\",\"c\":[1,2],\"d\":\"eeeeeeeeeeeeeeeeeeee\"}";
		assertExceptionLeavesOutputUntouched(new MaxSizeJsonFilter(string.length() - 1), string, metrics);
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
