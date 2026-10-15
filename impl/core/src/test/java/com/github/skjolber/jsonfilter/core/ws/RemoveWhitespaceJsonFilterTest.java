package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;

public class RemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	public RemoveWhitespaceJsonFilterTest() throws Exception {
		super();
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
		// the last string is not terminated
		String invalid = "{ \"a\" : \"b\" , \"c\" : [ 1 , 2 ] , \"d\" : \"eeeeeeeeeeeeeeeeeeee";
		RemoveWhitespaceJsonFilter filter = new RemoveWhitespaceJsonFilter();

		StringBuilder builder = new StringBuilder("PREFIX|");
		assertFalse(filter.process(invalid.toCharArray(), 0, invalid.length(), builder));
		assertEquals("PREFIX|", builder.toString());

		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		output.write("PREFIX|".getBytes(StandardCharsets.UTF_8));
		assertFalse(filter.process(invalid.getBytes(StandardCharsets.UTF_8), 0, invalid.length(), output));
		assertEquals("PREFIX|", output.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new RemoveWhitespaceJsonFilter().process(TRUNCATED));
		assertNull(new RemoveWhitespaceJsonFilter().process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
}
