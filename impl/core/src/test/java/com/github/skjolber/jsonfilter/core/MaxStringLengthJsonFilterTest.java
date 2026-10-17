package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_returns_false_with_metrics() throws Exception {
		// Processing invalid input with metrics tracking must return false rather than throw an exception.
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertFalse(new MaxStringLengthJsonFilter(10).process(new char[] {}, 1, 1, new StringBuilder(), metrics));
		assertFalse(new MaxStringLengthJsonFilter(10).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

	@Test
	public void maxStringLengthTopLevelString() throws Exception {
		// a document which is a single string has no structural character after the string
		String string = "\"hello world, a long string\"";
		String truncated = "\"hello... + 21\"";
		MaxStringLengthJsonFilter filter = new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH);

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertEquals(truncated, filter.process(string));
		assertEquals(truncated, filter.process(string, metrics));
		assertArrayEquals(truncated.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8)));
		assertArrayEquals(truncated.getBytes(StandardCharsets.UTF_8), filter.process(string.getBytes(StandardCharsets.UTF_8), metrics));
		assertEquals(2, metrics.getMaxStringLength());

		// a colon after the slice must not make the string look like a key
		String input = "x" + string + ":y";

		StringBuilder builder = new StringBuilder();
		assertTrue(filter.process(input.toCharArray(), 1, string.length(), builder));
		assertEquals(truncated, builder.toString());

		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(input.getBytes(StandardCharsets.UTF_8), 1, string.length(), output));
		assertEquals(truncated, output.toString(StandardCharsets.UTF_8));
	}
	

	@Test
	public void testProcessStringReturnsInputIfUnchanged() {
		MaxStringLengthJsonFilter filter = new MaxStringLengthJsonFilter(8);

		String unchanged = "{\"a\":\"short\",\"b\":[1,2]}";
		assertSame(unchanged, filter.process(unchanged));

		String changed = "{\"a\":\"longer than eight\",\"b\":[1,2]}";
		String output = filter.process(changed);
		assertNotSame(changed, output);
		assertEquals(filter.process(changed.toCharArray()), output);
	}
}
