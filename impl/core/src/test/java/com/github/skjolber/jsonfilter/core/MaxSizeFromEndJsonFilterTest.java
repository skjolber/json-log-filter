package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

/**
 * Verifies that {@link MaxSizeFromEndJsonFilter} produces identical output to
 * {@link MaxSizeJsonFilter} for all inputs, while scanning backward rather than
 * forward to locate the truncation cut point.
 */
public class MaxSizeFromEndJsonFilterTest extends DefaultJsonFilterTest {

	/** Forces processMaxSizeFromEnd even when length <= maxSize. */
	private static class MustConstrainFilter extends MaxSizeFromEndJsonFilter {
		public MustConstrainFilter(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	}

	/** Forces processMaxSize even when length <= maxSize (used for comparison). */
	private static class MustConstrainForwardFilter extends MaxSizeJsonFilter {
		public MustConstrainForwardFilter(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	}

	public MaxSizeFromEndJsonFilterTest() throws Exception {
		super();
	}

	// -----------------------------------------------------------------------
	// Helper: assert backward == forward for both char[] and byte[]
	// -----------------------------------------------------------------------

	private static void assertSameAsForwardFilter(String json, int maxSize) {
		MustConstrainFilter backward = new MustConstrainFilter(maxSize);
		MustConstrainForwardFilter forward = new MustConstrainForwardFilter(maxSize);

		char[] chars = json.toCharArray();
		StringBuilder sbBackward = new StringBuilder();
		StringBuilder sbForward = new StringBuilder();
		assertTrue(backward.process(chars, 0, chars.length, sbBackward), "backward char failed");
		assertTrue(forward.process(chars, 0, chars.length, sbForward), "forward char failed");
		assertEquals(sbForward.toString(), sbBackward.toString(),
				"char[] mismatch for maxSize=" + maxSize + " json=" + json);

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream outBackward = new ResizableByteArrayOutputStream(256);
		ResizableByteArrayOutputStream outForward = new ResizableByteArrayOutputStream(256);
		assertTrue(backward.process(bytes, 0, bytes.length, outBackward), "backward byte failed");
		assertTrue(forward.process(bytes, 0, bytes.length, outForward), "forward byte failed");
		assertEquals(outForward.toString(), outBackward.toString(),
				"byte[] mismatch for maxSize=" + maxSize + " json=" + json);
	}

	// -----------------------------------------------------------------------
	// pass-through / exception guards
	// -----------------------------------------------------------------------

	@Test
	public void testProcessStringReturnsInputWhenDocumentFits() {
		String json = "{\"key\":\"value\",\"list\":[1,2,3]}";

		MaxSizeFromEndJsonFilter filter = new MaxSizeFromEndJsonFilter(json.length());
		assertSame(json, filter.process(json));

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertSame(json, filter.process(json, metrics));
		assertEquals(json.length(), metrics.getInputSize());
		assertEquals(json.length(), metrics.getOutputSize());

		// one character too long: filtered as before
		MaxSizeFromEndJsonFilter smaller = new MaxSizeFromEndJsonFilter(json.length() - 1);
		assertEquals(smaller.process(json.toCharArray()), smaller.process(json));
		assertEquals(smaller.process(json.toCharArray(), new DefaultJsonFilterMetrics()), smaller.process(json, new DefaultJsonFilterMetrics()));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeFromEndJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void testNonStructuralIsKept() {
		// there is no smaller valid document
		MaxSizeFromEndJsonFilter filter = new MaxSizeFromEndJsonFilter(2);
		for (String json : new String[] {"\"abc\"", "-1.5e3", "123456", "true", "false", "null"}) {
			assertEquals(json, filter.process(json));
			assertEquals(json, new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeFromEndJsonFilter(-1).process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeFromEndJsonFilter(-1).process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
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
		assertExceptionLeavesOutputUntouched(new MaxSizeFromEndJsonFilter(string.length() - 1), string, metrics);
	}

	@Test
	public void maxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustConstrainFilter(size);
		assertThat(maxSize, new DefaultJsonFilter()).hasMaxSize();
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxSizeFromEndJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxSizeFromEndJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — flat array
	// -----------------------------------------------------------------------

	@Test
	public void testFlatArray_smallMaxSize() {
		String json = "[1,2,3]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testFlatArray_withSpaces() {
		String json = "[1, 2, 3]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testFlatArray_entireArrayForced() {
		// MustConstrainFilter forces the algorithm even when length == maxSize
		String json = "[1,2,3]";
		assertSameAsForwardFilter(json, json.length());
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — nested arrays
	// -----------------------------------------------------------------------

	@Test
	public void testNestedArray_variousMaxSizes() {
		String json = "[[1,2],[3,4]]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testDeeplyNestedArray() {
		String json = "[[[1,2],[3,4]],[[5,6],[7,8]]]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — objects
	// -----------------------------------------------------------------------

	@Test
	public void testFlatObject_variousMaxSizes() {
		String json = "{\"a\":1,\"b\":2,\"c\":3}";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testNestedObject_variousMaxSizes() {
		String json = "{\"x\":{\"a\":1,\"b\":2},\"y\":3}";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — array of objects
	// -----------------------------------------------------------------------

	@Test
	public void testArrayOfObjects_variousMaxSizes() {
		String json = "[{\"a\":1},{\"b\":2},{\"c\":3}]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — whitespace / markToLimit rescue
	// -----------------------------------------------------------------------

	@Test
	public void testWhitespace_trailingSpacesAfterValue() {
		String json = "[1, 2    ]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testWhitespace_spacesAroundBrackets() {
		String json = "[ { \"key\" : [ 1 , 2 ] } ]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — escaped quotes in strings
	// -----------------------------------------------------------------------

	@Test
	public void testEscapedQuotes_variousMaxSizes() {
		String json = "[\"say \\\"hi\\\"\",\"world\"]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — larger real-ish document
	// -----------------------------------------------------------------------

	@Test
	public void testLargerDocument() {
		String json = "[{\"id\":1,\"name\":\"Alice\",\"tags\":[\"admin\",\"user\"]},{\"id\":2,\"name\":\"Bob\",\"tags\":[\"user\"]}]";
		// Test a representative sample of maxSizes
		for (int maxSize = 1; maxSize <= json.length(); maxSize += 3) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — cut point inside a string
	// containing , ] or }
	// -----------------------------------------------------------------------

	@Test
	public void testStructuralCharactersInStrings() {
		String json = "{\"a\":\"x,y\"}";
		MaxSizeFromEndJsonFilter filter = new MaxSizeFromEndJsonFilter(8);
		assertEquals("{}", filter.process(json));
		assertEquals("{}", new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

		String log = "{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"],\"message\":\"Order 42 failed: [timeout, retry], see log\"}";
		for (int maxSize : new int[] {70, 78}) {
			filter = new MaxSizeFromEndJsonFilter(maxSize);
			assertEquals("{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"]}", filter.process(log));
			assertEquals("{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"]}", new String(filter.process(log.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testStructuralCharactersInStrings_variousMaxSizes() {
		String[] jsons = {
			"{\"a\":\"x,y\"}",
			"{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"],\"message\":\"Order 42 failed: [timeout, retry], see log\"}",
			"[\"a]b\",{\"c\" : \"d}e\"} , \"f,g\"]"
		};
		for (String json : jsons) {
			for (int maxSize = 1; maxSize < json.length(); maxSize++) {
				assertSameAsForwardFilter(json, maxSize);
			}
		}
	}

	// -----------------------------------------------------------------------
	// identical output to MaxSizeJsonFilter — whitespace after the root
	// -----------------------------------------------------------------------

	@Test
	public void testWhitespaceAfterRoot() {
		// the root structure fits, whitespace is kept up to the max size
		String json = "{\"level\":\"INFO\",\"message\":\"done\"}\r\n";
		MaxSizeFromEndJsonFilter filter = new MaxSizeFromEndJsonFilter(34);
		assertEquals("{\"level\":\"INFO\",\"message\":\"done\"}\r", filter.process(json));
		assertEquals("{\"level\":\"INFO\",\"message\":\"done\"}\r", new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

		json = "[1,2,3]      ";
		for (int maxSize = 8; maxSize <= 12; maxSize++) {
			filter = new MaxSizeFromEndJsonFilter(maxSize);
			assertEquals("[1,2,3]" + " ".repeat(maxSize - 7), filter.process(json));
			assertEquals("[1,2,3]" + " ".repeat(maxSize - 7), new String(filter.process(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testWhitespaceAfterRoot_variousMaxSizes() {
		String[] jsons = {
			"{\"level\":\"INFO\",\"message\":\"done\"}\r\n",
			"[1,2,3]      "
		};
		for (String json : jsons) {
			for (int maxSize = 1; maxSize < json.length(); maxSize++) {
				assertSameAsForwardFilter(json, maxSize);
			}
		}
	}

	// -----------------------------------------------------------------------
	// grow bracket arrays beyond initial capacity (>32 levels)
	// -----------------------------------------------------------------------

	@Test
	public void testGrowBracketArrays() throws Exception {
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustConstrainFilter filter = new MustConstrainFilter(400);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));

		// Also verify matches forward filter
		assertSameAsForwardFilter(json, 400);
	}

	// -----------------------------------------------------------------------
	// backward scan over quoted values
	// -----------------------------------------------------------------------

	@Test
	public void testScanBackwardBeyondQuotedValue() {
		// the scan checks four characters per iteration, so cover every length modulo four,
		// with escaped quotes and backslashes at different positions
		String[] values = new String[] {
			"", "a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg", "abcdefgh", "abcdefghi",
			"\\\"", "a\\\"", "a\\\"b", "\\\"abcd", "abcd\\\"", "\\\"\\\"\\\"",
			"\\\\", "a\\\\", "\\\\\\\"", "abc\\\\\\\"d", "\\\\\\\\\\\\"
		};
		String[] prefixes = new String[] {"", "x", "xx", "xxx", "{\"key\":"};

		for (String prefix : prefixes) {
			for (String value : values) {
				String json = prefix + '"' + value + '"';
				int closing = json.length() - 1;

				assertEquals(prefix.length(), MaxSizeFromEndJsonFilter.scanBackwardBeyondQuotedValue(json.toCharArray(), closing), json);
				assertEquals(prefix.length(), MaxSizeFromEndJsonFilter.scanBackwardBeyondQuotedValue(json.getBytes(StandardCharsets.UTF_8), closing), json);
			}
		}
	}

	// -----------------------------------------------------------------------
	// invalid / broken input
	// -----------------------------------------------------------------------

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, false), StandardCharsets.UTF_8);
		String broken = string.substring(0, string.length() / 2);

		MaxSizeFromEndJsonFilter filter = new MaxSizeFromEndJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));

		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));

		filter = new MaxSizeFromEndJsonFilter(brokenBytes.length);
		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}
}
