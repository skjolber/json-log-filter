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

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

/**
 * Truncating a string must not split an escaped surrogate pair (a high and a low surrogate \\uXXXX escape),
 * as that leaves a lone high surrogate which cannot be encoded as UTF-8.
 */
public class MaxStringLengthEscapedSurrogatePairTest {

	private static final String PAIR = "\\uD83D\\uDE00"; // escaped U+1F600

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

			assertWithMessage(entry.getKey() + " char[] " + maxStringLength).that(process(filter, input, false)).isEqualTo("{\"k\":\"" + expected + "\"}");
			assertWithMessage(entry.getKey() + " byte[] " + maxStringLength).that(process(filter, input, true)).isEqualTo("{\"k\":\"" + expected + "\"}");
		}

		// max size below the input size, but above the size of the truncated output, so that
		// the size-constrained code paths run. Only the truncated value is checked here.
		String nested = "{\"a\":{\"k\":\"" + value + "\"}}";
		for (Map.Entry<String, IntFunction<JsonFilter>> entry : maxSizeFilters(nested.length() - 2).entrySet()) {
			JsonFilter filter = entry.getValue().apply(maxStringLength);

			assertWithMessage(entry.getKey() + " char[] " + maxStringLength).that(process(filter, nested, false)).contains("{\"k\":\"" + expected + "\"}");
			assertWithMessage(entry.getKey() + " byte[] " + maxStringLength).that(process(filter, nested, true)).contains("{\"k\":\"" + expected + "\"}");
		}
	}

	@Test
	public void testTruncateWithinEscapedSurrogatePair() {
		String value = "ab" + PAIR + "cdefghijklmnopqrstuvwxyz0123";

		// cut within the high surrogate escape, between the two escapes, or within the low surrogate escape
		for(int maxStringLength = 3; maxStringLength <= 13; maxStringLength++) {
			assertFiltered(value, maxStringLength, "ab... + 40");
		}
		// cut after the pair
		assertFiltered(value, 14, "ab" + PAIR + "... + 28");
	}

	@Test
	public void testTruncateWithinLowerCaseEscapedSurrogatePair() {
		String value = "ab\\ud83d\\ude00cdefghijklmnopqrstuvwxyz0123";

		for(int maxStringLength = 3; maxStringLength <= 13; maxStringLength++) {
			assertFiltered(value, maxStringLength, "ab... + 40");
		}
		assertFiltered(value, 14, "ab\\ud83d\\ude00... + 28");
	}

	@Test
	public void testTruncateWithinConsecutiveEscapedSurrogatePairs() {
		String value = PAIR + PAIR + "cdefghijklmnopqrstuvwxyz0123";

		assertFiltered(value, 12, PAIR + "... + 40");
		for(int maxStringLength = 13; maxStringLength <= 23; maxStringLength++) {
			assertFiltered(value, maxStringLength, PAIR + "... + 40");
		}
		assertFiltered(value, 24, PAIR + PAIR + "... + 28");
	}

	@Test
	public void testTruncateAfterEscapedBackslashAndHighSurrogateText() {
		// escaped backslash followed by the plain text uD83D, which is not a high surrogate
		assertFiltered("a\\\\uD83Dbcdefghijklmnopqrstuvwxyz0123", 8, "a\\\\uD83D... + 29");
		// escaped backslash, followed by an escaped surrogate pair
		assertFiltered("a\\\\" + PAIR + "bcdefghijklmnopqrstuvwxyz0123", 9, "a\\\\... + 41");
	}

	@Test
	public void testStringAlignment() {
		// quote, two chars, then the pair at index 3 to 14
		String string = "\"ab" + PAIR + "cd\"";
		char[] chars = string.toCharArray();
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

		assertThat(CharArrayRangesFilter.getStringAlignment(chars, 3)).isEqualTo(3);
		assertThat(ByteArrayRangesFilter.getStringAlignment(bytes, 3)).isEqualTo(3);
		for(int start = 4; start < 15; start++) {
			assertWithMessage(Integer.toString(start)).that(CharArrayRangesFilter.getStringAlignment(chars, start)).isEqualTo(3);
			assertWithMessage(Integer.toString(start)).that(ByteArrayRangesFilter.getStringAlignment(bytes, start)).isEqualTo(3);
		}
		assertThat(CharArrayRangesFilter.getStringAlignment(chars, 15)).isEqualTo(15);
		assertThat(ByteArrayRangesFilter.getStringAlignment(bytes, 15)).isEqualTo(15);

		// escaped high surrogate followed by an escaped character which is not a low surrogate
		String bmp = "\"ab\\uD83D\\u00e9cd\"";
		assertThat(CharArrayRangesFilter.getStringAlignment(bmp.toCharArray(), 9)).isEqualTo(3);
		assertThat(ByteArrayRangesFilter.getStringAlignment(bmp.getBytes(StandardCharsets.UTF_8), 9)).isEqualTo(3);

		// cut right after other escapes stays
		String nonSurrogate = "\"ab\\uDC00\\uE000\\u0800cd\"";
		for(int start : new int[] {9, 15, 21}) {
			assertThat(CharArrayRangesFilter.getStringAlignment(nonSurrogate.toCharArray(), start)).isEqualTo(start);
			assertThat(ByteArrayRangesFilter.getStringAlignment(nonSurrogate.getBytes(StandardCharsets.UTF_8), start)).isEqualTo(start);
		}
	}

	@Test
	public void testTruncateAtEveryLength() throws Exception {
		// escaped pairs in upper and lower case, an escaped BMP character, and raw (unescaped) pairs
		String raw = "a" + PAIR + "b\\ud83d\\ude00\\u00e9" + PAIR + "\uD83D\uDE00c\\\\" + PAIR + "\\uDBFF\\uDFFF\uD83D\uDE00" + "0123456789abcdef0123456789";
		String input = "{\"k\":\"" + raw + "\"}";

		for(int maxStringLength = 1; maxStringLength < raw.getBytes(StandardCharsets.UTF_8).length; maxStringLength++) {
			for (Map.Entry<String, IntFunction<JsonFilter>> entry : filters().entrySet()) {
				JsonFilter filter = entry.getValue().apply(maxStringLength);

				String message = entry.getKey() + " " + maxStringLength;

				assertTruncated(message + " char[]", raw, maxStringLength, process(filter, input, false), false);
				assertTruncated(message + " byte[]", raw, maxStringLength, process(filter, input, true), true);
			}
		}
	}

	private static void assertTruncated(String message, String raw, int maxStringLength, String output, boolean bytes) throws Exception {
		assertWithMessage(message).that(output).isNotNull();
		message = message + " -> " + output;

		assertWithMessage(message).that(output).startsWith("{\"k\":\"");
		assertWithMessage(message).that(output).endsWith("\"}");

		// decoded value must be well-formed UTF-16, i.e. without lone surrogates
		String decoded = decode(output);
		for(int i = 0; i < decoded.length(); i++) {
			char c = decoded.charAt(i);
			if(Character.isHighSurrogate(c)) {
				i++;
				assertWithMessage(message).that(i < decoded.length() && Character.isLowSurrogate(decoded.charAt(i))).isTrue();
			} else {
				assertWithMessage(message).that(Character.isLowSurrogate(c)).isFalse();
			}
		}

		String value = output.substring(6, output.length() - 2);
		if(value.equals(raw)) {
			return;
		}
		int index = value.lastIndexOf("... + ");
		assertWithMessage(message).that(index).isAtLeast(0);

		String prefix = value.substring(0, index);
		int removed = Integer.parseInt(value.substring(index + 6));

		assertWithMessage(message).that(raw).startsWith(prefix);

		int rawLength;
		int prefixLength;
		if(bytes) {
			rawLength = raw.getBytes(StandardCharsets.UTF_8).length;
			prefixLength = prefix.getBytes(StandardCharsets.UTF_8).length;
		} else {
			rawLength = raw.length();
			prefixLength = prefix.length();
		}
		// at most a pair of escapes is dropped
		assertWithMessage(message).that(prefixLength).isAtLeast(maxStringLength - 11);
		assertWithMessage(message).that(prefixLength).isAtMost(maxStringLength);
		assertWithMessage(message).that(removed).isEqualTo(rawLength - prefixLength);
	}

	private static String decode(String json) throws Exception {
		try (JsonParser parser = new JsonFactory().createParser(json)) {
			JsonToken token;
			while((token = parser.nextToken()) != null) {
				if(token == JsonToken.VALUE_STRING) {
					return parser.getText();
				}
			}
		}
		throw new IllegalArgumentException(json);
	}
}
