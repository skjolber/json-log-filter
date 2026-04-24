package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class MaxStringLengthMaxSizeRemoveWhitespaceJsonFilterTest  extends DefaultJsonFilterTest {

	private static class MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter {
		public MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize) {
			super(maxStringLength, maxSize);
		}
		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	}

	public MaxStringLengthMaxSizeRemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size));
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
		assertThat(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThat(maxSize, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

	@Test
	public void maxSizeWhitespaceHeavy() throws Exception {
		MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, DEFAULT_MAX_SIZE);

		String json = "{ \"key\" : \"value\" , \"other\" : \"data\" }";
		StringBuilder sb = new StringBuilder();
		boolean result = filter.process(json.toCharArray(), 0, json.length(), sb);
		if (result) {
			org.junit.jupiter.api.Assertions.assertTrue(sb.length() > 0);
		}
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSizeWithStringLengthExceeded() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, size));
	}

	@Test
	public void testMustConstrainExceptionReturnsFalse() {
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 100);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testMustConstrainClosingBraceMaxSizeLimit() {
		// When a closing bracket increases the size limit to exactly the document length, the filter transitions to the unconstrained path.
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 9);
		String json = "{\"k\":\"v\"}";
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testMustConstrainWhitespaceMaxSizeLimit() {
		// Skipping many whitespace characters can bring the remaining size limit up to the document length, causing the filter to transition to unconstrained processing.
		String json = "{\"k\":          \"v\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 10);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testMustConstrainKeyWithWhitespaceBeforeColon() {
		// Whitespace before a key colon is skipped, and if doing so brings the size limit to the document length, the filter transitions to unconstrained processing.
		String json = "{\"longlonglong\"     :\"v\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 21);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testMustConstrainKeyWithWhitespaceAfterColon() {
		// Long key (>= maxStringLength) with whitespace ONLY after colon
		// {"longlonglong": "v"} = 22 chars, maxSize=21, maxStringLength=5
		String json = "{\"longlonglong\": \"v\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 21);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testMustConstrainBracketLevelGreaterThanZero() {
		// When the size limit is hit inside a nested structure, the filter exits with unmatched brackets and closes them before returning the truncated result.
		String json = "{\"k\": \"v\" }";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 9);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testMustConstrainWithMetrics() {
		// Test the metrics branch in process(char/byte, ..., JsonFilterMetrics)
		String json = "{\"key\": \"longvalue\", \"other\": \"data\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, 100);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb, metrics));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut, metrics));
	}

	@Test
	public void testMustConstrainLongValueTruncated() {
		// A value longer than the string limit is truncated when the filter operates in size-constrained mode.
		String json = "{\"k\":\"longvalue\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, 1000);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testStreamMarkWithMultipleValues() throws Exception {
		// After processing an earlier field, the stream mark is updated; a subsequent long value correctly picks up the updated mark position.
		String json = "{\"k1\":\"short\",\"k2\":\"longlonglong\",\"k3\":\"v\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, 100);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testValueMaxSizeLimitReached() throws Exception {
		// Truncating a long value can make the remaining size limit cover the rest of the document, causing the filter to transition to unconstrained processing.
		String json = "{\"k\":\"longlonglongvalue\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, json.length() - 5);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}
	@Test
	public void testMarkLimitFound() throws Exception {
		// When the size limit is reached inside a nested structure, the filter correctly closes open brackets and returns a well-formed truncated result.
		String json = "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 18);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testKeyWhitespaceBeforeColonMaxSizeReached() throws Exception {
		// Whitespace before a key colon is skipped, and if doing so brings the size limit to the document length, the filter transitions to unconstrained processing.
		String json = "{\"longlonglong\"   :\"v\"}";
		int totalLen = json.length();
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, totalLen - 4);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testWhitespaceAtStartCausingMaxSizeLimitBranchWithValue() throws Exception {
		// Whitespace after a key colon is skipped, and if doing so brings the size limit to the document length, the filter transitions to unconstrained processing.
		String json = "{\"k\"  :   \"longlonglong\"}";
		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(5, json.length() - 6);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testGrowSquareBracketsInProcess() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// maxSize is set just below the full document length so the filter runs in size-constrained mode.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x", false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		int maxSize = json.length() - 1;

		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testBracketLevelZeroAtLoopExit() throws Exception {
		// When maxSize is zero the processing loop never runs, and the filter correctly returns an empty output.
		String json = "{\"k\":\"v\"}";

		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, 0);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testWhitespaceAfterColonCausesMaxSizeLimitOverflow() throws Exception {
		// Whitespace between a key colon and its value is skipped by the filter; this verifies the filter handles the case where doing so causes the remaining document to fit within the size limit.
		String json = "{\"k\":  \"longlonglongvalue\"}";
		int maxSize = json.length() - 3; // tight: loop processes until near end

		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(3, maxSize);
		StringBuilder sb = new StringBuilder();
		filter.process(json.toCharArray(), 0, json.length(), sb);

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		filter.process(jsonBytes, 0, jsonBytes.length, byteOut);
	}

}
