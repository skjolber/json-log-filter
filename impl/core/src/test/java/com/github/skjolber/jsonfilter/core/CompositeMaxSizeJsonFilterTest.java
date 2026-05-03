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

/**
 * Tests that {@link CompositeMaxSizeJsonFilter} produces output identical to
 * {@link MaxSizeJsonFilter} and that it selects the correct scan direction at
 * every maxSize, including the crossover point near {@code length / 2}.
 */
public class CompositeMaxSizeJsonFilterTest extends DefaultJsonFilterTest {

	/** Forces the constraint path even when length <= maxSize. */
	private static class MustConstrainFilter extends CompositeMaxSizeJsonFilter {
		public MustConstrainFilter(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	}

	/** Reference: forces forward constraint. */
	private static class MustConstrainForwardFilter extends MaxSizeJsonFilter {
		public MustConstrainForwardFilter(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean mustConstrainMaxSize(int length) {
			return true;
		}
	}

	public CompositeMaxSizeJsonFilterTest() throws Exception {
		super();
	}

	// -----------------------------------------------------------------------
	// pass-through / exception guards
	// -----------------------------------------------------------------------

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new CompositeMaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new CompositeMaxSizeJsonFilter(-1).process(new char[]{}, 1, 1, new StringBuilder()));
		assertFalse(new CompositeMaxSizeJsonFilter(-1).process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new CompositeMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new CompositeMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void maxSize() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MustConstrainFilter(size);
		assertThat(maxSize, new DefaultJsonFilter()).hasMaxSize();
	}

	// -----------------------------------------------------------------------
	// full integration test from resource file
	// -----------------------------------------------------------------------

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new CompositeMaxSizeJsonFilter(size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure((size) -> new CompositeMaxSizeJsonFilter(size));
	}

	// -----------------------------------------------------------------------
	// helper: assert composite == forward for both char[] and byte[]
	// -----------------------------------------------------------------------

	private static void assertSameAsForwardFilter(String json, int maxSize) {
		MustConstrainFilter composite = new MustConstrainFilter(maxSize);
		MustConstrainForwardFilter forward = new MustConstrainForwardFilter(maxSize);

		char[] chars = json.toCharArray();
		StringBuilder sbComposite = new StringBuilder();
		StringBuilder sbForward = new StringBuilder();
		assertTrue(composite.process(chars, 0, chars.length, sbComposite), "composite char failed");
		assertTrue(forward.process(chars, 0, chars.length, sbForward), "forward char failed");
		assertEquals(sbForward.toString(), sbComposite.toString(),
				"char[] mismatch for maxSize=" + maxSize + " json=" + json);

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream outComposite = new ResizableByteArrayOutputStream(256);
		ResizableByteArrayOutputStream outForward = new ResizableByteArrayOutputStream(256);
		assertTrue(composite.process(bytes, 0, bytes.length, outComposite), "composite byte failed");
		assertTrue(forward.process(bytes, 0, bytes.length, outForward), "forward byte failed");
		assertEquals(outForward.toString(), outComposite.toString(),
				"byte[] mismatch for maxSize=" + maxSize + " json=" + json);
	}

	// -----------------------------------------------------------------------
	// scan-direction selection: uses forward scan when maxSize <= length/2,
	// backward scan when maxSize > length/2, identical output either way
	// -----------------------------------------------------------------------

	@Test
	public void testFlatArray_allMaxSizes() {
		// Exercises the full crossover: at length/2 the filter switches from backward to forward
		String json = "[1,2,3,4,5]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testFlatArray_crossoverPoint() {
		// length=11; crossover at maxSize=5 (forward) vs maxSize=6 (backward)
		String json = "[1,2,3,4,5]";
		assertSameAsForwardFilter(json, 5);   // maxSize == length - maxSize: use forward
		assertSameAsForwardFilter(json, 6);   // maxSize  > length - maxSize: use backward
	}

	@Test
	public void testNestedObject_allMaxSizes() {
		String json = "{\"a\":{\"b\":1,\"c\":2},\"d\":3}";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testArrayOfObjects_allMaxSizes() {
		String json = "[{\"a\":1},{\"b\":2},{\"c\":3}]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testLargerDocument_representativeSizes() {
		String json = "[{\"id\":1,\"name\":\"Alice\",\"tags\":[\"admin\",\"user\"]},{\"id\":2,\"name\":\"Bob\",\"tags\":[\"user\"]}]";
		for (int maxSize = 1; maxSize <= json.length(); maxSize += 3) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testWhitespace() {
		String json = "[ { \"key\" : [ 1 , 2 ] } ]";
		for (int maxSize = 1; maxSize < json.length(); maxSize++) {
			assertSameAsForwardFilter(json, maxSize);
		}
	}

	@Test
	public void testGrowBracketArrays() throws Exception {
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, "x".repeat(500), false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		MustConstrainFilter filter = new MustConstrainFilter(400);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb));

		ResizableByteArrayOutputStream byteOut = new ResizableByteArrayOutputStream(512);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOut));

		assertSameAsForwardFilter(json, 400);
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, false), StandardCharsets.UTF_8);
		String broken = string.substring(0, string.length() / 2);

		CompositeMaxSizeJsonFilter filter = new CompositeMaxSizeJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));

		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));

		filter = new CompositeMaxSizeJsonFilter(brokenBytes.length);
		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}
}
