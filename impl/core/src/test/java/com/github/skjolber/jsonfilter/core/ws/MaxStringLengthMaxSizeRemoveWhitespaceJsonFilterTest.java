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
		// JSON where the closing '}' pushes maxSizeLimit to maxReadLimit
		// {"k":"v"} = 9 chars, maxSize=9 -> '{' decrements to 8; when '}' encountered, maxSizeLimit++=9=maxReadLimit
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
		// JSON with many spaces such that whitespace skipping drives maxSizeLimit to maxReadLimit
		// {"k": followed by 10 spaces then "v"} - total ~19 chars, maxSize=10
		// The 10 whitespace chars are skipped, incrementing maxSizeLimit each time -> maxSizeLimit reaches maxReadLimit
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
		// Long key (>= maxStringLength) with whitespace before colon, where whitespace skip
		// causes maxSizeLimit to reach maxReadLimit
		// {"longlonglong"     :"v"} = 25 chars, maxSize=21, maxStringLength=5
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
		// JSON where the loop exits with bracketLevel > 0 (partial processing)
		// {"k": "v" } = 12 chars, maxSize=9 -> loop exits after "v" with bracketLevel > 0
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
		// Long VALUE (not key) with MustConstrain - covers the value truncation path
		// maxStringLength=3 means "longvalue" (9 chars) should be truncated
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
		// Multiple key-value pairs: after processing "k1":"short", mark is updated.
		// Then "k2":"longlonglong" → flushedOffset <= mark → streamMark update path.
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
		// Value whose addMaxLength causes maxSizeLimit >= maxReadLimit
		// maxStringLength=3, maxSize = large but close to total size
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
		// bracketLevel > 0 at loop exit, mark <= maxSizeLimit, markToLimit returns a valid value
		// Use multi-key JSON with tight maxSize so the loop exits partway, then markLimit path is taken
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
		// Key with whitespace before colon, maxSizeLimit >= maxReadLimit after skip
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
		// Value where after whitespace skip causes maxSizeLimit >= maxReadLimit (line 169-173)
		// Long string value prefixed by whitespace and close to the maxSize boundary
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
		// 35 nested brackets triggers grow() in processMaxStringLengthMaxSize (bracketLevel >= 32)
		// maxSize=JSON_LEN-1 to avoid crash when maxSizeLimit > maxReadLimit
		StringBuilder deepJson = new StringBuilder();
		for (int i = 0; i < 35; i++) {
			deepJson.append("{\"k").append(String.format("%02d", i)).append("\":");
		}
		deepJson.append("\"x\"");
		for (int i = 0; i < 35; i++) {
			deepJson.append("}");
		}
		String json = deepJson.toString();
		int maxSize = json.length() - 1;

		MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter filter =
			new MustConstrainMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, maxSize);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));
	}

	@Test
	public void testBracketLevelZeroAtLoopExit() throws Exception {
		// Covers the 'else' branch when bracketLevel==0 at end of loop (lines 259-260 char, 487 byte)
		// This happens when maxSize=0 (or maxSizeLimit=0): loop never executes, bracketLevel stays 0
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
		// Key with whitespace AFTER colon: after skipping whitespace, maxSizeLimit >= maxReadLimit
		// This covers lines 223-225 (char) and 451-453 (byte)
		// Need tight maxSize so after skipping post-colon whitespace, maxSizeLimit covers rest
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
