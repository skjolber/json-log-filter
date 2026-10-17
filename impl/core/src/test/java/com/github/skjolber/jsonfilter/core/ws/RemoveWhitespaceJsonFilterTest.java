package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

public class RemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	public RemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testProcessReturnsInputWithoutWhitespace() {
		RemoveWhitespaceJsonFilter filter = new RemoveWhitespaceJsonFilter();

		// whitespace inside strings is kept, so this document is returned as it is
		String compact = "{\"a b\":\"c \\\" d\",\"e\":[1,2]}";
		assertSame(compact, filter.process(compact));

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertSame(compact, filter.process(compact, metrics));
		assertEquals(compact.length(), metrics.getInputSize());
		assertEquals(compact.length(), metrics.getOutputSize());

		byte[] bytes = compact.getBytes(StandardCharsets.UTF_8);
		byte[] output = filter.process(bytes);
		assertArrayEquals(bytes, output);
		assertNotSame(bytes, output);

		// from an offset
		byte[] offsetBytes = ("xx" + compact + "yy").getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(bytes, filter.process(offsetBytes, 2, bytes.length));
	}

	@Test
	public void testProcessWithWhitespaceLikeCharArrays() {
		RemoveWhitespaceJsonFilter filter = new RemoveWhitespaceJsonFilter();

		String[] documents = new String[] {
			" {\"a\":1}", "{\"a\": 1}", "{\"a\":1} ", "\n{ \"a\" : [ 1 , 2 ] }\n", "{\"a b\":\"c\"}\t"
		};
		for(String document : documents) {
			// the regular path, through a char array and an output stream
			String expected = filter.process(document.toCharArray());
			assertEquals(expected, filter.process(document), document);

			DefaultJsonFilterMetrics expectedMetrics = new DefaultJsonFilterMetrics();
			filter.process(document.toCharArray(), expectedMetrics);
			DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
			assertEquals(expected, filter.process(document, metrics), document);
			assertEquals(expectedMetrics.getInputSize(), metrics.getInputSize());
			assertEquals(expectedMetrics.getOutputSize(), metrics.getOutputSize());

			byte[] bytes = document.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream stream = new ResizableByteArrayOutputStream(16);
			assertTrue(filter.process(bytes, 0, bytes.length, stream));
			assertArrayEquals(stream.toByteArray(), filter.process(bytes), document);
		}

		// invalid input gives null, as on the regular path
		assertNull(filter.process("{\"a\":\"b"));
		assertNull(filter.process("{\"a\":\"b".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		RemoveWhitespaceJsonFilter filter = new RemoveWhitespaceJsonFilter();

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new RemoveWhitespaceJsonFilter();

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new RemoveWhitespaceJsonFilter()).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new RemoveWhitespaceJsonFilter().process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new RemoveWhitespaceJsonFilter().process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_leaves_output_untouched() throws Exception {
		assertExceptionLeavesOutputUntouched(new RemoveWhitespaceJsonFilter(), UNTERMINATED_LAST_STRING);
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new RemoveWhitespaceJsonFilter().process(TRUNCATED));
		assertNull(new RemoveWhitespaceJsonFilter().process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
}
