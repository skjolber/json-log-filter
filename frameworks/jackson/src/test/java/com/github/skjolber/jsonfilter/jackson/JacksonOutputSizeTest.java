package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

public class JacksonOutputSizeTest {

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
				int accurateSize = JacksonOutputSize.getAccurateSize(parser, fieldName, token, utf8);

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
					sizes.add(JacksonOutputSize.getSeparatorSize(parser, token));
				}
			}
		}
		// tokens: [ 1 [ 2 ] { 3 [ ] } 4 ]
		assertEquals(List.of(0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0), sizes);
	}

	@Test
	public void testPrefixSize() throws IOException {
		// separator, and field name and colon
		String json = "[1,{\"a\":2,\"b\u00e6\":3}]";

		List<Integer> chars = new ArrayList<>();
		List<Integer> bytes = new ArrayList<>();
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			String fieldName = null;
			JsonToken token;
			while((token = parser.nextToken()) != null) {
				if(token == JsonToken.PROPERTY_NAME) {
					fieldName = parser.currentName();
					continue;
				}
				chars.add(JacksonOutputSize.getPrefixSize(parser, token, fieldName, false));
				bytes.add(JacksonOutputSize.getPrefixSize(parser, token, fieldName, true));
				fieldName = null;
			}
		}
		// tokens: [ 1 { 2 3 } ]
		assertEquals(List.of(0, 0, 1, 4, 6, 0, 0), chars);
		assertEquals(List.of(0, 0, 1, 4, 7, 0, 0), bytes);
	}

	@Test
	public void testQuotedSize() throws IOException {
		// quotes, escapes and, for UTF-8 output, multi-byte characters
		String[] values = new String[] {"", "abc", "\"\\/", "\b\t\n\f\r", "\u0000\u0001\u001f", "æ", "€", "😀", "a\"æ\t€😀"};
		for(String value : values) {
			char[] chars = ("xx" + value + "xx").toCharArray();
			for(boolean utf8 : new boolean[] {false, true}) {
				int size = writtenSize((generator) -> generator.writeString(value), utf8);

				assertEquals(size, JacksonOutputSize.getQuotedSize(value, utf8), value);
				assertEquals(size, JacksonOutputSize.getQuotedSize(chars, 2, value.length(), utf8), value);
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
			assertEquals(value.length(), JacksonOutputSize.getRawSize(value.toCharArray(), false), value);
			assertEquals(value.getBytes(StandardCharsets.UTF_8).length, JacksonOutputSize.getRawSize(value.toCharArray(), true), value);
		}
	}

	@Test
	public void testIsUtf8() throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(new StringBuilder()))) {
			assertFalse(JacksonOutputSize.isUtf8(generator));
		}
		try (JsonGenerator generator = jsonFactory.createGenerator(new ResizableByteArrayOutputStream(16))) {
			assertTrue(JacksonOutputSize.isUtf8(generator));
		}
	}

	@Test
	public void testAccurateMaxStringSize() throws IOException {
		String[] documents = new String[] {
			"{\"a\":\"abcdefghijklmnopqrstuvwxyz\",\"b\":\"abc\"}", // truncated and not truncated
			"[\"abcdefghijklmnopqrstuvwxyz\",\"abcdefghijklmnopqrstuvwxyz\"]", // separator before truncated value
			"{\"\\tæ\":\"abcdefghijklmnopqrstuvwxyz\"}", // escapes and multi-byte UTF-8 characters in field name
			"{\"k\":\"" + "\\t".repeat(20) + "\"}", // escapes in the kept part
			"{\"k\":\"" + "æ".repeat(20) + "\"}", // multi-byte UTF-8 characters in the kept part
			"{\"k\":\"abcd\uD83D\uDE00fghijklmnopqrstu\"}", // surrogate pair at the cut
			"{\"k\":\"æææææææ\"}" // not truncated, as the truncate message is longer than the removed part
		};
		for(String document : documents) {
			// also a truncate message with a multi-byte UTF-8 character
			for(String truncateMessage : new String[] {"... + ", "\u2026 + "}) {
				assertAccurateMaxStringSize(document, 5, truncateMessage.toCharArray(), false);
				assertAccurateMaxStringSize(document, 5, truncateMessage.toCharArray(), true);
			}
		}
	}

	/**
	 * Copy the document token by token, truncating long strings like the filter does, and check that
	 * the accurate size of each token is the size the generator writes for it.
	 */

	private static void assertAccurateMaxStringSize(String json, int maxStringLength, char[] truncateStringValue, boolean utf8) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		StringBuilder chars = new StringBuilder();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		StringBuilder builder = new StringBuilder();
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
				int accurateSize = JacksonOutputSize.getAccurateMaxStringSize(parser, fieldName, token, maxStringLength, truncateStringValue, JacksonOutputSize.getRawSize(truncateStringValue, utf8), utf8);

				int size = utf8 ? bytes.size() : chars.length();
				if(fieldName != null) {
					generator.writeName(fieldName);
					fieldName = null;
				}
				if(token == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
					JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				} else {
					JacksonEvents.copyCurrentEvent(parser, generator);
				}
				generator.flush();

				int written = (utf8 ? bytes.size() : chars.length()) - size;
				assertEquals(written, accurateSize, json + " " + token + " " + new String(truncateStringValue) + (utf8 ? " UTF-8" : ""));
			}
		}
	}

}
