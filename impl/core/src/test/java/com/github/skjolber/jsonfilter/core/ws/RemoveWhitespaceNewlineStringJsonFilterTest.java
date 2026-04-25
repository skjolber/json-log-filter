package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
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
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/newlineString/objectKeyNewline.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		// newline should be replaced with space
		assertFalse(charOutput.toString().contains("\n"));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertFalse(new String(byteOutput.toByteArray(), StandardCharsets.UTF_8).contains("\n"));
	}

	@Test
	public void testEscapedQuoteAfterBackslash() throws Exception {
		// JSON string ending with \\\" - backslash before quote
		// "value\\\"" means the string ends with backslash+quote (where \" is escaped)
		// This covers the even-number-of-slashes logic
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/newlineString/objectKeyEscapedQuote.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testWithMetrics() throws Exception {
		// Test the metrics branch (when metrics != null) in both char and byte variants
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/newlineString/objectKeyNewlineOther.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, metrics));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, metrics));
	}

	@Test
	public void testExceptionWithMetricsReturnsFalse() throws Exception {
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertFalse(filter.process(new char[]{}, 1, 1, new StringBuilder(), metrics));
		assertFalse(filter.process(new byte[]{}, 1, 1, new ResizableByteArrayOutputStream(128), metrics));
	}

	@Test
	public void testStringWithEscapedBackslashBeforeClosingQuote() throws Exception {
		// Lines 79 (char) and 153 (byte): `break` when even number of backslashes before `"`
		// JSON string value "\\\\" = string containing two backslashes: value ends with \\
		// `chars[offset-1]='\'` triggers the chain. Two backslashes → count=2, 3%2=1 → break
		RemoveWhitespaceNewlineStringJsonFilter filter = new RemoveWhitespaceNewlineStringJsonFilter();
		String json2 = "{\"key\":\"a\\\\\\\\\"}";
		byte[] json2Bytes = json2.getBytes(StandardCharsets.UTF_8);
		assertNotNull(filter.process(json2.toCharArray(), 0, json2.length(), new StringBuilder()));
		assertNotNull(filter.process(json2Bytes));
	}


}
