package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;

public class RemoveWhitespaceNewlineStringJsonFilterTest extends DefaultJsonFilterTest {

	public RemoveWhitespaceNewlineStringJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new RemoveWhitespaceNewlineStringJsonFilter();

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new RemoveWhitespaceNewlineStringJsonFilter()).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new RemoveWhitespaceNewlineStringJsonFilter().process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new RemoveWhitespaceNewlineStringJsonFilter().process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new RemoveWhitespaceNewlineStringJsonFilter().process(TRUNCATED));
		assertNull(new RemoveWhitespaceNewlineStringJsonFilter().process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
}
