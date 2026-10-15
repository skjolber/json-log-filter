package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class MaxStringLengthMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	private static class MustContrainMaxStringLengthMaxSizeJsonFilter extends MaxStringLengthMaxSizeJsonFilter {
		public MustContrainMaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize) {
			super(maxStringLength, maxSize);
		}
		public MustContrainMaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize,
				String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
		}
		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	};

	public MaxStringLengthMaxSizeJsonFilterTest() throws Exception {
		super();
	}
	
	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxStringLengthMaxSizeJsonFilter(128, size));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(-1, size);

		assertThat(maxSize, new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThat(maxSize, new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

	@Test
	public void maxStringLengthTopLevelStringLongerThanMaxSize() throws Exception {
		// a string cannot be cut, so it is kept as-is, or truncated by max string length
		String string = "\"hello world, a long string\"";
		String truncated = "\"hello... + 21\"";

		JsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(-1, 10);
		assertEquals(string, filter.process(string));
		assertArrayEquals(string.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8)));

		filter = new MaxStringLengthMaxSizeJsonFilter(DEFAULT_MAX_STRING_LENGTH, 10);
		assertEquals(truncated, filter.process(string));
		assertArrayEquals(truncated.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testTopLevelNumberOrLiteralIsKept() {
		// there is no smaller valid document
		MaxStringLengthMaxSizeJsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(5, 2);
		for(String json : new String[] {"-1.5e3", "123456", "true", "false", "null"}) {
			assertEquals(json, filter.process(json));
			assertEquals(json, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		}
	}
	
	@Test
	public void exception_returns_false() throws Exception {
		JsonFilter filter = new MaxStringLengthMaxSizeJsonFilter(-1, -1);
		assertFalse(filter.process(new char[] {}, 1, 1, new StringBuilder()));
		assertNull(filter.process(new byte[] {}, 1, 1));
	}

	@Test
	public void testGrowSquareBrackets() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity.
		// A long leaf value ensures the size limit is reached during content processing, not before opening all brackets.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(5, 400);
		assertNotNull(filter.process(json.toCharArray(), 0, json.length(), new StringBuilder()));

		assertNotNull(filter.process(jsonBytes));
	}

	@Test
	public void testExceptionCaughtInRanges() throws Exception {
		// An invalid input offset causes the filter to return false rather than throw.
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 100);
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void testLongKeyIsNotTruncated() throws Exception {
		// A field name that is longer than maxStringLength is not truncated — only values are truncated.
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 20);
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectLongKeyValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		StringBuilder charOutput1 = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput1));
		assertEquals("{}", charOutput1.toString());
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
		StringBuilder charOutput2 = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput2));
		assertEquals("{}", charOutput2.toString());
		MustContrainMaxStringLengthMaxSizeJsonFilter filterB = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, 22);
		byte[] byteResult = filterB.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{}", new String(byteResult, StandardCharsets.UTF_8));
	}

	@Test
	public void testLongValueRemoveLastFilter() throws Exception {
		// When a long value is truncated and the truncation message itself is longer than the remaining allowed size,
		// the filter removes the last partial entry to stay within bounds.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/maxSize/objectKAbcN.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3,
			jsonBytes.length - 1,
			"\"***SKIPPED***\"", "\"***\"", "X".repeat(50));
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"k\":\"abcdefg\"}", charOutput.toString());
		byte[] byteResult = filter.process(jsonBytes);
		assertNotNull(byteResult);
		assertEquals("{\"k\":\"abcdefg\"}", new String(byteResult, StandardCharsets.UTF_8));
	}

	// a long value followed by whitespace, which is truncated to {"o":{"k":"aaa... + 17"}} (25 chars) for max string length 3
	private static final String LONG_VALUE_TRAILING_WHITESPACE = "{\"o\":{\"k\":\"" + "a".repeat(20) + "\"" + " ".repeat(40) + ",\"x\":1}}";

	@Test
	public void testRangesMaxSizeMaxStringLengthKeepsTruncatedValueWhenWhitespaceAfterItDoesNotFit() {
		// from max size 25 the truncated value fits, but the whitespace after it does not fit until max size 65:
		// keep the truncation and mark right after the value, so that the cut does not include the raw value
		int markAfterValue = LONG_VALUE_TRAILING_WHITESPACE.indexOf(' ');
		for(int maxSize = 25; maxSize < 65; maxSize++) {
			assertRangesMaxSizeMaxStringLength("{\"o\":{\"k\":\"aaa... + 17\"}}", maxSize, markAfterValue);
		}
	}

	@Test
	public void testRangesMaxSizeMaxStringLengthRemovesTruncationWhenTruncatedValueDoesNotFit() {
		// at max size 24 not even the truncated value fits: remove the truncation and mark before the field
		int markBeforeField = LONG_VALUE_TRAILING_WHITESPACE.indexOf("\"k\"");
		assertRangesMaxSizeMaxStringLength("{\"o\":{}}", 24, markBeforeField);
	}

	private static void assertRangesMaxSizeMaxStringLength(String expected, int maxSize, int expectedMark) {
		String json = LONG_VALUE_TRAILING_WHITESPACE;
		int comma = json.indexOf(',');

		char[] chars = json.toCharArray();
		CharArrayRangesSizeFilter charFilter = new CharArrayRangesSizeFilter(-1, chars.length, "\"[removed]\"".toCharArray(), "\"*\"".toCharArray(), "... + ".toCharArray());
		charFilter.setMark(0);
		assertEquals(comma, MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, 0, chars.length, maxSize, 3 + 2, charFilter));
		assertEquals(expectedMark, charFilter.getMark(), Integer.toString(maxSize));

		// cut the rest of the document at the mark, as ranges(..) does
		charFilter.addDelete(charFilter.getMark(), chars.length);
		StringBuilder builder = new StringBuilder();
		charFilter.filter(chars, 0, chars.length, builder);
		assertEquals(expected, builder.toString(), Integer.toString(maxSize));

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ByteArrayRangesSizeFilter byteFilter = new ByteArrayRangesSizeFilter(-1, bytes.length, "\"[removed]\"".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8), "... + ".getBytes(StandardCharsets.UTF_8));
		byteFilter.setMark(0);
		assertEquals(comma, MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(bytes, 0, bytes.length, maxSize, 3 + 2, byteFilter));
		assertEquals(expectedMark, byteFilter.getMark(), Integer.toString(maxSize));

		byteFilter.addDelete(byteFilter.getMark(), bytes.length);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		byteFilter.filter(bytes, 0, bytes.length, output);
		assertEquals(expected, new String(output.toByteArray(), StandardCharsets.UTF_8), Integer.toString(maxSize));
	}

	@Test
	public void testLongKeyWithWhitespaceBeforeColon() throws Exception {
		// A long key followed by multiple whitespace characters before the colon is handled without truncating the key.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/irregularWhitespace/objectLongKeySpacedValue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MustContrainMaxStringLengthMaxSizeJsonFilter filter = new MustContrainMaxStringLengthMaxSizeJsonFilter(3, jsonBytes.length - 1);
		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{}", charOutput.toString());
		byte[] byteResult2 = filter.process(jsonBytes);
		assertNotNull(byteResult2);
		assertEquals("{}", new String(byteResult2, StandardCharsets.UTF_8));
	}


}
