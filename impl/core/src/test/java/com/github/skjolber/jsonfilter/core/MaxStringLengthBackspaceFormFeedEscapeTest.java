package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;

/**
 * Truncating a string right after a \b or \f escape must drop the whole escape. The letters
 * 'b' and 'f' are also hex digits, so they must not be mistaken for part of a \\uXXXX escape.
 */
public class MaxStringLengthBackspaceFormFeedEscapeTest {

	private static Map<String, IntFunction<JsonFilter>> filters() {
		Map<String, IntFunction<JsonFilter>> filters = new LinkedHashMap<>();
		filters.put("maxStringLength", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).build());
		filters.put("maxStringLength+removeWhitespace", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withRemoveWhitespace(true).build());
		filters.put("path+maxStringLength", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withAnonymizePaths("$.other").build());
		filters.put("path+maxStringLength+removeWhitespace", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withAnonymizePaths("$.other").withRemoveWhitespace(true).build());
		return filters;
	}

	private static Map<String, IntFunction<JsonFilter>> maxSizeFilters(int maxSize) {
		Map<String, IntFunction<JsonFilter>> filters = new LinkedHashMap<>();
		filters.put("maxStringLength+maxSize", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withMaxSize(maxSize).build());
		filters.put("maxStringLength+maxSize+removeWhitespace", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withMaxSize(maxSize).withRemoveWhitespace(true).build());
		filters.put("path+maxStringLength+maxSize", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withAnonymizePaths("$.other").withMaxSize(maxSize).build());
		filters.put("path+maxStringLength+maxSize+removeWhitespace", n -> DefaultJsonLogFilterBuilder.newBuilder().withMaxStringLength(n).withAnonymizePaths("$.other").withMaxSize(maxSize).withRemoveWhitespace(true).build());
		return filters;
	}

	private static String process(JsonFilter filter, String input, boolean bytes) {
		if(bytes) {
			byte[] output = filter.process(input.getBytes(StandardCharsets.UTF_8));
			return output == null ? null : new String(output, StandardCharsets.UTF_8);
		}
		return filter.process(input);
	}

	/**
	 * @param value raw (escaped) string value
	 * @param maxStringLength max string length
	 * @param expected expected raw (escaped) string value, including the truncate message
	 */
	private static void assertFiltered(String value, int maxStringLength, String expected) {
		String input = "{\"k\":\"" + value + "\"}";
		for (Map.Entry<String, IntFunction<JsonFilter>> entry : filters().entrySet()) {
			JsonFilter filter = entry.getValue().apply(maxStringLength);

			assertWithMessage(entry.getKey() + " char[]").that(process(filter, input, false)).isEqualTo("{\"k\":\"" + expected + "\"}");
			assertWithMessage(entry.getKey() + " byte[]").that(process(filter, input, true)).isEqualTo("{\"k\":\"" + expected + "\"}");
		}

		// max size below the input size, but above the size of the truncated output, so that
		// the size-constrained code paths run. Only the truncated value is checked here.
		String nested = "{\"a\":{\"k\":\"" + value + "\"}}";
		for (Map.Entry<String, IntFunction<JsonFilter>> entry : maxSizeFilters(nested.length() - 2).entrySet()) {
			JsonFilter filter = entry.getValue().apply(maxStringLength);

			assertWithMessage(entry.getKey() + " char[]").that(process(filter, nested, false)).contains("{\"k\":\"" + expected + "\"}");
			assertWithMessage(entry.getKey() + " byte[]").that(process(filter, nested, true)).contains("{\"k\":\"" + expected + "\"}");
		}
	}

	@Test
	public void testTruncateAfterBackspaceEscape() {
		assertFiltered("aaaa\\bcccccccccccccccccccc", 5, "aaaa... + 22");
		assertFiltered("\\b\\b\\b\\b\\b\\b", 1, "... + 12");
		assertFiltered("\\b\\b\\b\\b\\b\\b\\b\\b", 3, "\\b... + 14");
	}

	@Test
	public void testTruncateAfterFormFeedEscape() {
		assertFiltered("aaaa\\fcccccccccccccccccccc", 5, "aaaa... + 22");
		assertFiltered("\\f\\f\\f\\f\\f\\f", 1, "... + 12");
		assertFiltered("\\f\\f\\f\\f\\f\\f\\f\\f", 3, "\\f... + 14");
	}

	@Test
	public void testTruncateAfterEscapedBackslashFollowedByHexLetter() {
		// escaped backslash, then a plain 'b' or 'f': cutting before the letter keeps the escaped backslash
		assertFiltered("aaa\\\\bcccccccccccccccccccc", 5, "aaa\\\\... + 21");
		assertFiltered("aaa\\\\fcccccccccccccccccccc", 5, "aaa\\\\... + 21");
		// escaped backslash, then \b: cutting inside the \b escape drops the whole escape
		assertFiltered("aa\\\\\\bcccccccccccccccccccc", 5, "aa\\\\... + 22");
	}

	@Test
	public void testTruncateAfterEscapedBackslashAndLiteralU() {
		// escaped backslash followed by a plain 'u', so the \b which follows is a separate escape
		assertFiltered("\\\\u\\bcccccccccccccccccccc", 4, "\\\\u... + 22");
	}

	@Test
	public void testStringAlignment() {
		// cut at the 'b' or 'f' of the escape moves the cut to the backslash
		String[] strings = new String[] {
			"\"aaaa\\bccc\"",
			"\"aaaa\\fccc\"",
			"\"\\\\u\\bccc\"",
		};
		for(String string : strings) {
			int index = string.lastIndexOf('\\'); // backslash of the \b or \f escape

			assertThat(CharArrayRangesFilter.getStringAlignment(string.toCharArray(), index + 1)).isEqualTo(index);
			assertThat(ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), index + 1)).isEqualTo(index);

			// cut after the escape stays
			assertThat(CharArrayRangesFilter.getStringAlignment(string.toCharArray(), index + 2)).isEqualTo(index + 2);
			assertThat(ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), index + 2)).isEqualTo(index + 2);
		}

		// escaped backslash followed by a plain 'b' or 'f': cut before the letter stays
		for(String string : new String[] {"\"aaa\\\\bccc\"", "\"aaa\\\\fccc\""}) {
			assertThat(CharArrayRangesFilter.getStringAlignment(string.toCharArray(), 6)).isEqualTo(6);
			assertThat(ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), 6)).isEqualTo(6);
		}
	}

	@Test
	public void testTruncateAtEveryLength() {
		String[] escapes = new String[] {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\"", "\\\\", "\\/"};

		for(String escape : escapes) {
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < 4; i++) {
				builder.append('a');
				builder.append(escape);
				builder.append(escape);
				builder.append("bf");
			}
			builder.append("0123456789abcdef0123456789");
			String raw = builder.toString();
			String input = "{\"k\":\"" + raw + "\"}";

			for(int maxStringLength = 1; maxStringLength < raw.length(); maxStringLength++) {
				for (Map.Entry<String, IntFunction<JsonFilter>> entry : filters().entrySet()) {
					JsonFilter filter = entry.getValue().apply(maxStringLength);

					String message = entry.getKey() + " " + input + " " + maxStringLength;

					assertTruncated(message + " char[]", raw, maxStringLength, process(filter, input, false));
					assertTruncated(message + " byte[]", raw, maxStringLength, process(filter, input, true));
				}
			}
		}
	}

	private static void assertTruncated(String message, String raw, int maxStringLength, String output) {
		assertWithMessage(message).that(output).isNotNull();
		assertWithMessage(message + " -> " + output).that(JsonValidator.isWellformed(output)).isTrue();

		assertWithMessage(message).that(output).startsWith("{\"k\":\"");
		assertWithMessage(message).that(output).endsWith("\"}");
		String value = output.substring(6, output.length() - 2);
		if(value.equals(raw)) {
			return;
		}
		int index = value.lastIndexOf("... + ");
		assertWithMessage(message).that(index).isAtLeast(0);

		String prefix = value.substring(0, index);
		int removed = Integer.parseInt(value.substring(index + 6));

		assertWithMessage(message).that(raw).startsWith(prefix);
		// at most the escape at the cut point is dropped
		assertWithMessage(message).that(prefix.length()).isAtLeast(maxStringLength - 1);
		assertWithMessage(message).that(prefix.length()).isAtMost(maxStringLength);
		assertWithMessage(message).that(removed).isEqualTo(raw.length() - prefix.length());
	}
}
