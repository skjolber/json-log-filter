package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
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
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeFromEndJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeFromEndJsonFilter(-1).process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeFromEndJsonFilter(-1).process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
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
