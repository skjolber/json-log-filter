package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class PrettyPrintingJsonFilterTest extends DefaultJsonFilterTest {

	public PrettyPrintingJsonFilterTest() throws Exception {
		super(false, true, true, true);
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
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new RemoveWhitespaceJsonFilter();

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(getPrettyPrinter()).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(getPrettyPrinter().process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(getPrettyPrinter().process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(getPrettyPrinter().process(TRUNCATED));
		assertNull(getPrettyPrinter().process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGrowSquareBracketsWithMetrics() throws Exception {
		// 35 levels of nesting forces the filter's bracket-tracking array to grow beyond its initial capacity in both char and byte paths.
		byte[] jsonBytes = Generator.generateDeepObjectStructure(35, false);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		PrettyPrintingJsonFilter filter = getPrettyPrinter();
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, metrics));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(512);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, metrics));
	}

	@Test
	public void testWhitespaceInInput() throws Exception {
		// Input JSON with structural whitespace - covers if(chars[offset] > 0x20) == false path
		PrettyPrintingJsonFilter filter = getPrettyPrinter();
		String json = "{  \"key\"  :  \"value\"  }";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, null));
		assertTrue(charOutput.toString().contains("key"));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, null));
	}

	@Test
	public void testEmptyArray() throws Exception {
		// JSON with empty array [] - covers the empty-array shortcut in second switch
		PrettyPrintingJsonFilter filter = getPrettyPrinter();
		String json = "{\"arr\":[]}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, null));
		assertTrue(charOutput.toString().contains("[]"));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, null));
	}

	@Test
	public void testEmptyArrayWithWhitespace() throws Exception {
		// JSON with empty array [ ] (space inside) - covers while loop in empty array detection
		PrettyPrintingJsonFilter filter = getPrettyPrinter();
		String json = "{\"arr\":[ ]}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, null));
		assertTrue(charOutput.toString().contains("[]"));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, null));
	}

	@Test
	public void testNonAsciiBytes() throws Exception {
		// JSON with non-ASCII UTF-8 character - covers chars[offset] < 0 path in byte version
		PrettyPrintingJsonFilter filter = getPrettyPrinter();
		String json = "{\"key\":\"caf\u00e9\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, null));
		// The non-ASCII bytes ('é' = 0xC3 0xA9) should be written via the chars[offset] < 0 path
		assertTrue(byteOutput.size() > 0);
	}


}
