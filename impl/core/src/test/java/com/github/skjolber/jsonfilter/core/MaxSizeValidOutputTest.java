package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.jackson.JsonValidator;

import tools.jackson.core.json.JsonFactory;

/**
 * Regression tests for max-size filters which produced invalid JSON, an empty or null result,
 * or leaked values which should have been anonymized, for valid input.
 */

public class MaxSizeValidOutputTest {

	private static final String SECRET_ARRAY = "{\"secret\":[1,2,3,42,5],\"other\":\"" + "o".repeat(40) + "\"}";
	private static final String SECRET_STRINGS = "{\"secret\":[\"S,SN\",\"P]W\"],\"other\":\"" + "o".repeat(40) + "\"}";
	private static final String SECRET_OBJECT = "{\"secret\":{\"pin\":\"1,2\",\"pw\":\"a}b\"},\"other\":\"" + "o".repeat(40) + "\"}";
	private static final String CARD = "{\"card\":{\"pin\":7,\"x\":8},\"other\":\"xxxxxxxxxx\"}";

	private static DefaultJsonLogFilterBuilder builder() {
		return DefaultJsonLogFilterBuilder.newBuilder();
	}

	/** Assert the same output for char[] and byte[] input. */
	private static void assertOutput(JsonFilter filter, String input, String expected) {
		assertWithMessage("char output for " + input).that(filter.process(input)).isEqualTo(expected);

		byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
		assertWithMessage("byte output for " + input).that(bytes).isNotNull();
		assertWithMessage("byte output for " + input).that(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(expected);
	}

	/** Assert well-formed, non-empty output for every max size, which does not contain any of the forbidden texts. */
	private static void assertValidForAllMaxSizes(IntFunction<JsonFilter> filters, String input, String... forbidden) {
		JsonFactory jsonFactory = new JsonFactory();
		byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
		for(int maxSize = 2; maxSize <= inputBytes.length; maxSize++) {
			JsonFilter filter = filters.apply(maxSize);

			String chars = filter.process(input);
			byte[] bytes = filter.process(inputBytes);

			assertWithMessage("char output for max size " + maxSize).that(chars).isNotNull();
			assertWithMessage("byte output for max size " + maxSize).that(bytes).isNotNull();

			assertWithMessage("char output for max size " + maxSize).that(chars.isBlank()).isFalse();
			assertWithMessage("byte output for max size " + maxSize).that(bytes.length).isGreaterThan(0);

			assertWithMessage("char output " + chars + " for max size " + maxSize).that(JsonValidator.isWellformed(chars, jsonFactory)).isTrue();
			assertWithMessage("byte output " + new String(bytes, StandardCharsets.UTF_8) + " for max size " + maxSize).that(JsonValidator.isWellformed(bytes, jsonFactory)).isTrue();

			for(String f : forbidden) {
				assertWithMessage("char output for max size " + maxSize).that(chars).doesNotContain(f);
				assertWithMessage("byte output for max size " + maxSize).that(new String(bytes, StandardCharsets.UTF_8)).doesNotContain(f);
			}
		}
	}

	// 1: scanning from the end must not cut within a string containing , ] or }

	@Test
	public void maxSizeDoesNotCutWithinString() {
		assertOutput(builder().withMaxSize(8).build(), "{\"a\":\"x,y\"}", "{}");

		String log = "{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"],\"message\":\"Order 42 failed: [timeout, retry], see log\"}";
		assertOutput(builder().withMaxSize(70).build(), log, "{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"]}");
		assertOutput(builder().withMaxSize(78).build(), log, "{\"level\":\"INFO\",\"tags\":[\"a\",\"b\"]}");

		assertValidForAllMaxSizes((size) -> builder().withMaxSize(size).build(), log);
	}

	// 2: whitespace after the root structure must not result in empty output

	@Test
	public void maxSizeWithTrailingWhitespace() {
		assertOutput(builder().withMaxSize(34).build(), "{\"level\":\"INFO\",\"message\":\"done\"}\r\n", "{\"level\":\"INFO\",\"message\":\"done\"}\r");

		for(int maxSize = 8; maxSize <= 12; maxSize++) {
			assertOutput(builder().withMaxSize(maxSize).build(), "[1,2,3]      ", "[1,2,3]" + " ".repeat(maxSize - 7));
		}
	}

	// 3: no extra closing brackets when a truncated string makes the rest of the document fit

	@Test
	public void maxStringLengthMaxSizeClosesStructureOnce() {
		String nested = "{\"a\":{\"b\":\"" + "x".repeat(40) + "\"},\"c\":1}";
		assertOutput(builder().withMaxStringLength(5).withMaxSize(50).build(), nested, "{\"a\":{\"b\":\"xxxxx... + 35\"},\"c\":1}");

		for(int maxSize = 18; maxSize <= 22; maxSize++) {
			assertOutput(builder().withMaxStringLength(1).withMaxSize(maxSize).build(), "{\"k\":\"abcdefghijklmno\"}", "{\"k\":\"a... + 14\"}");
		}

		// same code reached from the path filter, after max path matches is used up
		String path = "{\"z\":1,\"a\":{\"b\":\"" + "x".repeat(40) + "\"},\"c\":1}";
		assertOutput(builder().withAnonymizePaths("$.z").withMaxPathMatches(1).withMaxStringLength(5).withMaxSize(44).build(), path, "{\"z\":\"*\",\"a\":{\"b\":\"xxxxx... + 35\"},\"c\":1}");
	}

	// 4: max path matches used up within the size limit

	@Test
	public void maxPathMatchesClosesStructureOnce() {
		for(int maxSize = 21; maxSize <= 23; maxSize++) {
			assertOutput(builder().withAnonymizePaths("$.a").withMaxPathMatches(1).withMaxSize(maxSize).build(), "{\"a\":[true,true],\"b\":1}", "{\"a\":[\"*\",\"*\"],\"b\":1}");
		}
	}

	// 5: max size reached within an anonymized subtree must not include the raw value

	@Test
	public void maxSizeWithinAnonymizedSubtree() {
		assertOutput(builder().withAnonymizePaths("$.secret").withMaxSize(15).build(), SECRET_ARRAY, "{\"secret\":[]}");
		assertOutput(builder().withAnonymizePaths("$.secret").withMaxSize(18).build(), SECRET_ARRAY, "{\"secret\":[\"*\"]}");
		assertOutput(builder().withAnonymizePaths("$.secret").withMaxSize(27).build(), SECRET_ARRAY, "{\"secret\":[\"*\",\"*\",\"*\"]}");

		assertOutput(builder().withAnonymizeKeys("secret").withMaxSize(15).build(), SECRET_ARRAY, "{\"secret\":[]}");
		assertOutput(builder().withAnonymizeKeys("secret").withMaxSize(18).build(), SECRET_ARRAY, "{\"secret\":[\"*\"]}");
		assertOutput(builder().withAnonymizeKeys("secret").withMaxSize(27).build(), SECRET_ARRAY, "{\"secret\":[\"*\",\"*\",\"*\"]}");

		assertOutput(builder().withAnonymizePaths("$.card").withMaxSize(18).build(), CARD, "{\"card\":{}}");

		// the only digits in the input are the anonymized values
		String[] digits = {"1", "2", "3", "4", "5", "7", "8"};
		assertValidForAllMaxSizes((size) -> builder().withAnonymizePaths("$.secret").withMaxSize(size).build(), SECRET_ARRAY, digits);
		assertValidForAllMaxSizes((size) -> builder().withAnonymizeKeys("secret").withMaxSize(size).build(), SECRET_ARRAY, digits);
		assertValidForAllMaxSizes((size) -> builder().withAnonymizePaths("$.secret").withMaxStringLength(50).withMaxSize(size).build(), SECRET_ARRAY, digits);

		// anonymize message longer than the anonymized values
		assertValidForAllMaxSizes((size) -> builder().withAnonymizePaths("$.card").withAnonymizeMessage("ANONYMIZED").withMaxSize(size).build(), CARD, digits);
		assertValidForAllMaxSizes((size) -> builder().withAnonymizeKeys("card").withAnonymizeMessage("ANONYMIZED").withMaxSize(size).build(), CARD, digits);
		assertValidForAllMaxSizes((size) -> builder().withAnonymizeKeys("card").withAnonymizeMessage("ANONYMIZED").withMaxSize(size).build(), "{\"card\":{\"pin\":\"7\",\"x\":\"8\"},\"other\":\"xxxxxxxxxx\"}", digits);
	}

	// 6: same as 5, but removing whitespace

	@Test
	public void maxSizeWithinAnonymizedSubtreeRemoveWhitespace() {
		assertOutput(builder().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(15).build(), SECRET_STRINGS, "{\"secret\":[]}");
		assertOutput(builder().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(19).build(), SECRET_STRINGS, "{\"secret\":[\"*\"]}");
		assertOutput(builder().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(21).build(), SECRET_OBJECT, "{\"secret\":{}}");

		assertValidForAllMaxSizes((size) -> builder().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(size).build(), SECRET_STRINGS, "S", "P");
		assertValidForAllMaxSizes((size) -> builder().withAnonymizePaths("$.secret").withRemoveWhitespace(true).withMaxSize(size).build(), SECRET_OBJECT, "1", "a}b");
	}

	@Test
	public void maxPathMatchesWithinAnonymizedSubtreeRemoveWhitespace() {
		// max path matches used up by a subtree which was cut by max size
		String json = "{\"secret\":{\"c\":\"S\",\"name\":\"SECRET\"}}  \n ";
		for(int maxSize = 13; maxSize <= 16; maxSize++) {
			assertOutput(builder().withAnonymizeKeys("secret").withMaxPathMatches(1).withRemoveWhitespace(true).withMaxSize(maxSize).build(), json, "{\"secret\":{}}");
		}
	}

	@Test
	public void wildcardPathAfterSiblingRemoveWhitespace() {
		// the sibling "b" also matches the wildcard
		String json = "{\"b\":1,\"y\":{\"tags\":{\"id\":\"x\"},\"secret\":null,\"message\":\"abc\"}}";
		assertOutput(builder().withAnonymizePaths("$.*.secret").withRemoveWhitespace(true).withMaxSize(59).build(), json, "{\"b\":1,\"y\":{\"tags\":{\"id\":\"x\"},\"secret\":\"*\"}}");
		assertOutput(builder().withAnonymizePaths("$.*.secret").withRemoveWhitespace(true).withMaxSize(60).build(), json, "{\"b\":1,\"y\":{\"tags\":{\"id\":\"x\"},\"secret\":\"*\",\"message\":\"abc\"}}");
	}

	// 7: no null result when anonymizing or truncating makes the document fit

	@Test
	public void documentFitsAfterFiltering() {
		String user = "{\"user\":{\"password\":\"abcd\"}}";
		for(int maxSize = 25; maxSize <= 27; maxSize++) {
			assertOutput(builder().withAnonymizeKeys("password").withMaxSize(maxSize).build(), user, "{\"user\":{\"password\":\"*\"}}");
			assertOutput(builder().withAnonymizePaths("$.user.password").withMaxSize(maxSize).build(), user, "{\"user\":{\"password\":\"*\"}}");
		}
		assertOutput(builder().withAnonymizeKeys("key").withMaxSize(14).build(), "[{\"key\":66163}]", "[{\"key\":\"*\"}]");

		String truncated = "{\"a\":[\"" + "x".repeat(20) + "\"]}";
		for(int maxSize = 19; maxSize <= 21; maxSize++) {
			assertOutput(builder().withMaxStringLength(1).withMaxSize(maxSize).build(), truncated, "{\"a\":[\"x... + 19\"]}");
		}
	}

	@Test
	public void documentFitsAfterFilteringWithinLargerArray() {
		// the filters must not read beyond the given length, here into an unterminated string
		String user = "{\"user\":{\"password\":\"abcd\"}}";
		String padded = "]]}}\"" + user + "\"x";

		JsonFilter filter = builder().withAnonymizeKeys("password").withMaxSize(26).build();

		StringBuilder builder = new StringBuilder();
		assertThat(filter.process(padded.toCharArray(), 5, user.length(), builder)).isTrue();
		assertThat(builder.toString()).isEqualTo("{\"user\":{\"password\":\"*\"}}");

		byte[] bytes = filter.process(padded.getBytes(StandardCharsets.UTF_8), 5, user.length());
		assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"user\":{\"password\":\"*\"}}");
	}

	// a top-level number, true, false or null larger than the max size is kept as-is

	@Test
	public void topLevelNumberOrLiteralIsKept() {
		List<IntFunction<JsonFilter>> filters = new ArrayList<>();
		for(boolean removeWhitespace : new boolean[] {false, true}) {
			filters.add((size) -> builder().withRemoveWhitespace(removeWhitespace).withMaxSize(size).build());
			filters.add((size) -> builder().withRemoveWhitespace(removeWhitespace).withMaxStringLength(5).withMaxSize(size).build());
			filters.add((size) -> builder().withRemoveWhitespace(removeWhitespace).withAnonymizePaths("$.a").withMaxSize(size).build());
			filters.add((size) -> builder().withRemoveWhitespace(removeWhitespace).withAnonymizePaths("$.a").withMaxStringLength(5).withMaxSize(size).build());
			filters.add((size) -> builder().withRemoveWhitespace(removeWhitespace).withAnonymizeKeys("a").withMaxSize(size).build());
		}
		for(IntFunction<JsonFilter> filter : filters) {
			for(String scalar : new String[] {"-1.5e3", "123456", "true", "false", "null"}) {
				assertOutput(filter.apply(2), scalar, scalar);
			}
			assertOutput(filter.apply(4), "-1.5e3", "-1.5e3");
		}

		// the forward and backward scans of the default max size filter
		assertOutput(new MaxSizeJsonFilter(4), "-1.5e3", "-1.5e3");
		assertOutput(new MaxSizeFromEndJsonFilter(4), "-1.5e3", "-1.5e3");
	}

}
