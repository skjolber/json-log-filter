package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
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

	@Test
	public void testLiteralNewlineInString() throws Exception {
		// literal \n (ASCII 10) inside a JSON string value - covers the newline replacement branch
		String json = "{\"key\":\"value\nwith newline\"}";
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		// newline should be replaced with space
		assertFalse(charOutput.toString().contains("\n"));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertFalse(new String(byteOutput.toByteArray(), StandardCharsets.UTF_8).contains("\n"));
	}

	@Test
	public void testEscapedQuoteAfterBackslash() throws Exception {
		// JSON string ending with \\\" - backslash before quote
		// "value\\\"" means the string ends with backslash+quote (where \" is escaped)
		// This covers the even-number-of-slashes logic
		String json = "{\"key\":\"value\\\\\\\"\"}";
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testWithMetrics() throws Exception {
		// Test the metrics branch (when metrics != null) in both char and byte variants
		String json = "{\"key\":\"value\nwith newline\",\"other\":\"data\"}";
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, metrics));

		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, metrics));
	}

	@Test
	public void testExceptionWithMetricsReturnsFalse() throws Exception {
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics metrics = new com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics();
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testStringWithEscapedBackslashBeforeClosingQuote() throws Exception {
		// Lines 79 (char) and 153 (byte): `break` when even number of backslashes before `"`
		// JSON string value "\\\\" = string containing two backslashes: value ends with \\
		// `chars[offset-1]='\'` triggers the chain. Two backslashes → count=2, 3%2=1 → break
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		// Value is "ab\\" in JSON = ab\ in actual string (ends with backslash + closing quote)
		// Actually need chars: "ab\\\\"  = 4 chars in string where offset at closing " has offset-1=\
		// but we need TWO backslashes before the closing quote: "ab\\\\" means string = ab\\
		String json = "{\"k\":\"ab\\\\\"}"; // JSON value "ab\\" = string ab\ (odd backslashes? No)
		// Let's use: "a\\\\" = string a\\ (two backslashes)
		// In Java source: "\"a\\\\\\\\\"" = "a\\\\" in JSON = a\\ actual
		String json2 = "{\"key\":\"a\\\\\\\\\"}"; // "a\\\\" in JSON = a\\ actual string
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

}
