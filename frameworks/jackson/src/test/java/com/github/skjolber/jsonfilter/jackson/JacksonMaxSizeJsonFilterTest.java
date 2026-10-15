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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
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
	public void testMaxSizeCountsUtf8BytesForByteOutput() throws Exception {
		// the first field is 18 characters but 27 bytes with brackets
		String json = "{\"a\":\"ææææææææææ\",\"b\":1}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		JacksonMaxSizeJsonFilter filter = new JacksonMaxSizeJsonFilter(18);

		assertEquals("{\"a\":\"ææææææææææ\"}", filter.process(json));
		assertEquals("{}", new String(filter.process(bytes), StandardCharsets.UTF_8));

		// characters are counted when the output is characters
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output));
		assertEquals("{\"a\":\"ææææææææææ\"}", output.toString());
	}

	@Test
	public void testAccurateSize() throws IOException {
		String[] documents = new String[] {
			"{\"a\":1,\"b\":2,\"c\":3}", // colon after field name, separator before field name
			"{\"a\":{\"b\":{\"c\":1}},\"d\":2}", // brackets
			"[1,[2,3],{\"x\":\"y\"},[{\"z\":[4,5]}]]", // separator before opening bracket
			"{\"t\":true,\"f\":false,\"n\":null,\"i\":-12,\"d\":1.5e10}",
			"{\"k\":\"\\t\\b\\f\\n\\r\\u0001\\\"\\\\\"}", // escapes in value
			"{\"\\t\\\"\\\\\":1}", // escapes in field name
			"{\"æ\":\"ææ\",\"€\":\"€€\",\"😀\":\"😀\"}" // multi-byte UTF-8 characters
		};
		for(String document : documents) {
			assertAccurateSize(document, false);
			assertAccurateSize(document, true);
		}
	}

	/**
	 * Copy the document token by token, like the filter does, and check that the accurate size
	 * of each token is the size the generator writes for it.
	 */

	private static void assertAccurateSize(String json, boolean utf8) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		StringBuilder chars = new StringBuilder();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (
			JsonParser parser = jsonFactory.createParser(json);
			JsonGenerator generator = utf8 ? jsonFactory.createGenerator(bytes) : jsonFactory.createGenerator(new StringBuilderWriter(chars))
			) {
			String fieldName = null;
			JsonToken token;
			while((token = parser.nextToken()) != null) {
				if(token == JsonToken.PROPERTY_NAME) {
					fieldName = parser.currentName();
					continue;
				}
				int accurateSize = JacksonMaxSizeJsonFilter.getAccurateSize(parser, fieldName, token, utf8);

				int size = utf8 ? bytes.size() : chars.length();
				if(fieldName != null) {
					generator.writeName(fieldName);
					fieldName = null;
				}
				JacksonEvents.copyCurrentEvent(parser, generator);
				generator.flush();

				int written = (utf8 ? bytes.size() : chars.length()) - size;
				assertEquals(written, accurateSize, json + " " + token + (utf8 ? " UTF-8" : ""));
			}
		}
	}

	@Test
	public void testSeparatorSize() throws IOException {
		// a comma precedes every value or opening bracket which is not the first in its array or object
		String json = "[1,[2],{\"a\":3,\"b\":[]},4]";

		List<Integer> sizes = new ArrayList<>();
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			JsonToken token;
			while((token = parser.nextToken()) != null) {
				if(token != JsonToken.PROPERTY_NAME) {
					sizes.add(JacksonMaxSizeJsonFilter.getSeparatorSize(parser, token));
				}
			}
		}
		// tokens: [ 1 [ 2 ] { 3 [ ] } 4 ]
		assertEquals(List.of(0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0), sizes);
	}

	@Test
	public void testQuotedSize() throws IOException {
		// quotes, escapes and, for UTF-8 output, multi-byte characters
		String[] values = new String[] {"", "abc", "\"\\/", "\b\t\n\f\r", "\u0000\u0001\u001f", "æ", "€", "😀", "a\"æ\t€😀"};
		for(String value : values) {
			char[] chars = ("xx" + value + "xx").toCharArray();
			for(boolean utf8 : new boolean[] {false, true}) {
				int size = writtenSize((generator) -> generator.writeString(value), utf8);

				assertEquals(size, JacksonMaxSizeJsonFilter.getQuotedSize(value, utf8), value);
				assertEquals(size, JacksonMaxSizeJsonFilter.getQuotedSize(chars, 2, value.length(), utf8), value);
			}
		}
	}

	private static int writtenSize(Consumer<JsonGenerator> writer, boolean utf8) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		if(utf8) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (JsonGenerator generator = jsonFactory.createGenerator(output)) {
				writer.accept(generator);
			}
			return output.size();
		}
		StringBuilder output = new StringBuilder();
		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			writer.accept(generator);
		}
		return output.length();
	}

	@Test
	public void testRawSize() {
		for(String value : new String[] {"\"*\"", "\"æ\"", "\"€\"", "\"😀\""}) {
			assertEquals(value.length(), JacksonMaxSizeJsonFilter.getRawSize(value.toCharArray(), false), value);
			assertEquals(value.getBytes(StandardCharsets.UTF_8).length, JacksonMaxSizeJsonFilter.getRawSize(value.toCharArray(), true), value);
		}
	}

	@Test
	public void testIsUtf8() throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(new StringBuilder()))) {
			assertFalse(JacksonMaxSizeJsonFilter.isUtf8(generator));
		}
		try (JsonGenerator generator = jsonFactory.createGenerator(new ResizableByteArrayOutputStream(16))) {
			assertTrue(JacksonMaxSizeJsonFilter.isUtf8(generator));
		}
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
