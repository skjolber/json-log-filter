package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;

public class PrettyPrintingJsonFilterTest extends DefaultJsonFilterTest {

	public PrettyPrintingJsonFilterTest() throws Exception {
		super(false, true, true);
	}
	
	public PrettyPrintingJsonFilter getPrettyPrinter() {
		
		Indent ident = Indent.newBuilder().build();
		
		return new PrettyPrintingJsonFilter(ident);
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		JsonFilter filter = getPrettyPrinter();

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ByteArrayOutputStream()));
		
		filter = new RemoveWhitespaceJsonFilter();

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ByteArrayOutputStream()));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(getPrettyPrinter()).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(getPrettyPrinter().process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(getPrettyPrinter().process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(getPrettyPrinter().process(TRUNCATED));
		assertNull(getPrettyPrinter().process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
}
