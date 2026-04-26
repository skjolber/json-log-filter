package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

public class MaxStringLengthRemoveWhitespaceJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthRemoveWhitespaceJsonFilterTest() throws Exception {
		super(true);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MaxStringLengthRemoveWhitespaceJsonFilter maxStringLengthJsonFilter = new MaxStringLengthRemoveWhitespaceJsonFilter(-1);
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED));
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertNull(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}

	@Test
	public void testWhitespaceAroundColon() throws Exception {
		// Long key + whitespace before/after colon triggers the whitespace-around-colon branches.
		// "value" (5 chars) is not truncated because the truncation message would be longer than
		// the original. "longvaluelongvalue" (18 chars) is truncated to "lon... + 15".
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/irregularWhitespace/objectLongKeyWhitespaceBothSides.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"longkeyname\":\"value\",\"k\":\"lon... + 15\"}", charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertEquals("{\"longkeyname\":\"value\",\"k\":\"lon... + 15\"}", byteOutput.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testWhitespaceBeforeColonOnly() throws Exception {
		// Long key followed by whitespace then colon (no whitespace after colon).
		// "value" (5 chars) is not truncated because the truncation message would be longer than the original.
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/irregularWhitespace/objectLongKeyWhitespaceBefore.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"longkeyname\":\"value\"}", charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertEquals("{\"longkeyname\":\"value\"}", byteOutput.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testWithMetrics() throws Exception {
		// Metrics branches are covered; "longvaluelongvalue" (18 chars) exceeds maxStringLength=3
		// and is truncated to "lon... + 15" (saving space compared to the original).
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/shortKey/objectKeyLongvaluelongvalue.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, metrics));
		assertEquals("{\"key\":\"lon... + 15\"}", charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, metrics));
		assertEquals("{\"key\":\"lon... + 15\"}", byteOutput.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testWhitespaceAfterColonOnly() throws Exception {
		// Long key with colon directly after its closing quote, then whitespace before the value.
		// "longvaluelongvalue" (18 chars) is truncated to "lon... + 15".
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/irregularWhitespace/objectLonglonglongSpaceAfter.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"longlonglong\":\"lon... + 15\"}", charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertEquals("{\"longlonglong\":\"lon... + 15\"}", byteOutput.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testLongKeyNoWhitespace() throws Exception {
		// Long key with colon directly after (no whitespace anywhere).
		// "longvaluelongvalue" (18 chars) is truncated to "lon... + 15".
		byte[] jsonBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/json/text/irregularWhitespace/objectLonglonglongNoSpace.json"));
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));
		assertEquals("{\"longlonglong\":\"lon... + 15\"}", charOutput.toString());

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
		assertEquals("{\"longlonglong\":\"lon... + 15\"}", byteOutput.toString(StandardCharsets.UTF_8));
	}


}
