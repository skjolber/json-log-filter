package com.github.skjolber.jsonfilter.jackson;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class JacksonMaxSizeJsonFilterTest extends AbstractDefaultJacksonJsonFilterTest {

	public JacksonMaxSizeJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeJsonFilter(size));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new byte[] {}, 1, 1, new StringBuilder()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(StringBuilderWriter.class))).thenThrow(new RuntimeException());
		when(jsonFactory.createGenerator(any(ByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		
		testConvenienceMethods(
			new JacksonMaxSizeJsonFilter(1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return true;
				}
				public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
					return true;
				}
			}, 
			new JacksonMaxSizeJsonFilter(1) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					return false;
				}
				public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
					return false;
				}
			},
			new JacksonMaxSizeJsonFilter(1, jsonFactory) {
				public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
					throw new RuntimeException();
				}
				@Override
				public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
					throw new RuntimeException();
				}
			}
		);
		
		testConvenienceMethods(
				new JacksonMaxSizeJsonFilter(1024) {
					public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
						return true;					
					}
					public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
						return true;	
					}

					@Override
					public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
						return true;	
					}
					
					public boolean process(byte[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
						return true;	
					}
				}, 
				new JacksonMaxSizeJsonFilter(1024) {
					public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
						return false;
					}
					public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
						return false;
					}

					@Override
					public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
						return false;
					}
					
					public boolean process(byte[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
						return false;
					}
				},
				new JacksonMaxSizeJsonFilter(1024, jsonFactory) {
					public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
						throw new RuntimeException();
					}
					
					@Override
					public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
						throw new RuntimeException();
					}

					
				}
			);
	}
	
	@Test
	public void testConstructor() {
		new JacksonMaxSizeJsonFilter(1024, "XXX", "YYY", "ZZZ");
	}

	@Test
	public void testProcessByteArrayResizableStreamPassthrough() throws Exception {
		// When maxSize >= input length, process(byte[], ResizableByteArrayOutputStream) delegates to super
		byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
		JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(1024);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(json, 0, json.length, output, null));
		assertEquals("{}", new String(output.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testProcessByteArrayResizableStreamInvalidBounds() throws Exception {
		// Verify bounds check: bytes.length < offset + length returns false
		byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
		JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(10);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertFalse(filter.process(json, 0, json.length + 1, output, null));
	}

	@Test
	public void testProcessByteArrayResizableStreamException() throws Exception {
		// Verify catch block in process(byte[], ResizableByteArrayOutputStream) by using a broken factory
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createGenerator(any(ResizableByteArrayOutputStream.class))).thenThrow(new RuntimeException());
		JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(1, jsonFactory);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(json, 0, json.length, output, null));
	}

	@Test
	public void testProcessByteArrayToStringBuilderWithRealFilter() throws Exception {
		// Call process(byte[], StringBuilder) on real filter with maxSize < length to exercise lambdas.
		// The size check fires before the pending field name is flushed to the generator,
		// leaving only the opening brace in output.
		byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
		JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(10);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(json, 0, json.length, sb, null));
		assertEquals("{}", sb.toString());
	}
	
	
}
