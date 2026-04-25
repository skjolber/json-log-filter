package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
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
		// Long key + whitespace before/after colon triggers the whitespace-around-colon branches
		String json = "{\"longkeyname\"  :  \"value\",\"k\"  :  \"longvaluelongvalue\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testWhitespaceBeforeColonOnly() throws Exception {
		// long key followed by whitespace THEN colon (no whitespace after colon)
		String json = "{\"longkeyname\"  :\"value\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testWithMetrics() throws Exception {
		// Test with non-null metrics to cover metrics branches
		String json = "{\"key\":\"longvaluelongvalue\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput, metrics));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		metrics = new DefaultJsonFilterMetrics();
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput, metrics));
	}

	@Test
	public void testWhitespaceAfterColonOnly() throws Exception {
		// Long key with colon DIRECTLY after closing quote, then whitespace before value
		// Covers the else-branch "was a key" path where nextOffset++ -> whitespace
		String json = "{\"longlonglong\": \"longvaluelongvalue\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}

	@Test
	public void testLongKeyNoWhitespace() throws Exception {
		// Long key with colon directly after, no whitespace - key length >= maxStringLength
		// Covers the else-branch "was a key" path where nextOffset++ -> no whitespace
		String json = "{\"longlonglong\":\"longvaluelongvalue\"}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		MaxStringLengthRemoveWhitespaceJsonFilter filter = new MaxStringLengthRemoveWhitespaceJsonFilter(3);

		StringBuilder charOutput = new StringBuilder();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), charOutput));

		ResizableByteArrayOutputStream byteOutput = new ResizableByteArrayOutputStream(128);
		assertTrue(filter.process(jsonBytes, 0, jsonBytes.length, byteOutput));
	}


}
