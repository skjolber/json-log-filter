package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class JacksonMaxStringLengthJsonFilterTest extends AbstractDefaultJacksonJsonFilterTest {

	public JacksonMaxStringLengthJsonFilterTest() throws Exception {
		super(true);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}

	@Test
	public void testGetterMethods() {
		// Verify that getPruneJsonValue, getAnonymizeJsonValue, getTruncateStringValue return the configured values
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(10, "prune", "anon", "truncate");
		assertArrayEquals("prune".toCharArray(), filter.getPruneJsonValue());
		assertArrayEquals("anon".toCharArray(), filter.getAnonymizeJsonValue());
		assertArrayEquals("truncate".toCharArray(), filter.getTruncateStringValue());
	}

	@Test
	public void testProcessByteArrayResizableStream() throws Exception {
		// Verify process(byte[], int, int, ResizableByteArrayOutputStream, metrics) works correctly.
		// "value" (5 chars) with maxStringLength=3: the truncation message "val... + 2" is longer than
		// the original "value", so the filter writes the original string unchanged.
		String json = "{\"key\":\"value\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(3);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertEquals("{\"key\":\"value\"}", new String(output.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testProcessByteArrayResizableStreamInvalidBounds() throws Exception {
		// Verify bounds check: bytes.length < offset + length returns false
		byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(-1);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertFalse(filter.process(bytes, 0, bytes.length + 1, output, null));
	}

	@Test
	public void testProcessCharArrayInvalidBounds() throws Exception {
		// Verify chars.length < offset + length returns false in char[] process method
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(-1);
		assertFalse(filter.process(new char[]{'{'}, 0, 2, new StringBuilder(), null));
	}

	@Test
	public void testProcessByteArrayStringBuilderInvalidBounds() throws Exception {
		// Verify bytes.length < offset + length returns false in byte[]-to-StringBuilder process method
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(-1);
		assertFalse(filter.process(new byte[]{'{'}, 0, 2, new StringBuilder(), null));
	}

	@Test
	public void testSurrogatePairAtTruncationBoundary() throws Exception {
		// String where the char at position maxStringLength is a low surrogate (\uDE00).
		// "ABCDE" (5) + high surrogate \uD83D (index 5) + low surrogate \uDE00 (index 6) + "FGHIJKLMNOPQRSTU" (16) = 23 chars.
		// With maxStringLength=6, textCharacters[offset+6] == '\uDE00' triggers the isLowSurrogate branch,
		// keeping only 5 chars so the surrogate pair is not split; removed = 18.
		String value = "ABCDE\uD83D\uDE00FGHIJKLMNOPQRSTU";
		String json = "{\"key\":\"" + value + "\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(6);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(256);
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertEquals("{\"key\":\"ABCDE... + 18\"}", new String(output.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testProcessByteArrayResizableStreamException() throws Exception {
		// Verify catch block in process(byte[], ResizableByteArrayOutputStream) is hit when factory throws
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(ResizableByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		JacksonMaxStringLengthJsonFilter filter = new JacksonMaxStringLengthJsonFilter(-1, jsonFactory);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(json, 0, json.length, output, null));
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMaxStringLengthJsonFilter(-1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) {
					return true;
				}
			}, 
			new JacksonMaxStringLengthJsonFilter(-1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) {
					return false;
				}
			},
			new JacksonMaxStringLengthJsonFilter(-1, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) {
					throw new RuntimeException();
				}
			}
		);
	}
	
}
