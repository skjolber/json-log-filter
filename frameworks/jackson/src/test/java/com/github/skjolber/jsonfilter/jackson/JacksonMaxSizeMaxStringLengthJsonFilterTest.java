package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class JacksonMaxSizeMaxStringLengthJsonFilterTest extends AbstractDefaultJacksonJsonFilterTest {

	public JacksonMaxSizeMaxStringLengthJsonFilterTest() throws Exception {
		super(false);
	}
	
	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeMaxStringLengthJsonFilter(-1, size));
	}

	@Test
	public void passthrough_success() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMaxSizeMaxStringLengthJsonFilter(-1, size);
		assertThat(maxSize, new JacksonMaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new JacksonMaxSizeMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		assertThat(maxSize, new JacksonMaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
			}, 
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
			},
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			}
		);
		
		testConvenienceMethods(
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1024) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
			}, 
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1024) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
			},
			new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1024) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
				public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
			}
		);
	}

	@Test
	public void testProcessByteArrayResizableStreamPassthrough() throws Exception {
		// When maxSize >= input length, process(byte[], ResizableByteArrayOutputStream) delegates to super
		byte[] json = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		JacksonMaxSizeMaxStringLengthJsonFilter filter = new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1024);
		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(json, 0, json.length, output, null));
		assertEquals("{}", new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	public void testProcessByteArrayResizableStreamException() throws Exception {
		// Verify catch block in process(byte[], ResizableByteArrayOutputStream) via broken factory
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		JacksonMaxSizeMaxStringLengthJsonFilter filter = new JacksonMaxSizeMaxStringLengthJsonFilter(512, 1, jsonFactory);
		com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream output =
				new com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream(64);
		byte[] json = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		assertFalse(filter.process(json, 0, json.length, output, null));
	}

	@Test
	public void testProcessByteArrayToStringBuilderWithRealFilter() throws Exception {
		// Call process(byte[], StringBuilder) on real filter with maxSize < length to exercise lambdas.
		// The value "value" (6 chars when quoted) combined with the field name exhausts maxSize before
		// the field name is flushed to the generator, leaving only the opening brace in output.
		byte[] json = "{\"key\":\"value\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		JacksonMaxSizeMaxStringLengthJsonFilter filter = new JacksonMaxSizeMaxStringLengthJsonFilter(3, 10);
		StringBuilder sb = new StringBuilder();
		filter.process(json, 0, json.length, sb, null);
		assertEquals("{}", sb.toString());
	}

	@Test
	public void testStringTruncationWithMetrics() throws Exception {
		// Trigger string truncation in the processing loop with non-null metrics (covers metrics.onMaxStringLength)
		String json = "{\"key\":\"" + "A".repeat(100) + "\"}";
		JacksonMaxSizeMaxStringLengthJsonFilter filter = new JacksonMaxSizeMaxStringLengthJsonFilter(5, 200);
		StringBuilder sb = new StringBuilder();
		com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics metrics =
				new com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics();
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), sb, metrics));
		assertEquals("{\"key\":\"AAAAA... + 95\"}", sb.toString());
	}

	@Test
	public void testSurrogatePairInAccurateMaxStringSizeCalculation() throws Exception {
		// When the input exceeds maxSize, the size check fires before flushing the pending field,
		// so only the opening brace is written.
		String value = "ABCDE\uD83D\uDE00FGHIJKLMNOPQRSTU";
		String json = "{\"key\":\"" + value + "\"}";
		JacksonMaxSizeMaxStringLengthJsonFilter filter = new JacksonMaxSizeMaxStringLengthJsonFilter(6, 10);
		StringBuilder sb = new StringBuilder();
		filter.process(json.toCharArray(), 0, json.length(), sb, null);
		assertEquals("{}", sb.toString());
	}

}
