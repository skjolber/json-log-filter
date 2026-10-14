package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

/**
 * A configured key which contains a backslash must be compared with the unescaped JSON key,
 * so it must not match a JSON key with an escape sequence of the same raw text.
 */

public class EscapedKeyMatchingJsonFilterTest {

	// the first key is an escaped newline, the second is a backslash followed by n
	private static final String INPUT = "{\"\\n\":\"secret\",\"\\\\n\":\"secretsecretsecret\"}";
	private static final String ANONYMIZED = "{\"\\n\":\"secret\",\"\\\\n\":\"*\"}";
	private static final String PRUNED = "{\"\\n\":\"secret\",\"\\\\n\":\"[removed]\"}";

	private static List<UnaryOperator<DefaultJsonLogFilterBuilder>> variants(int maxSize) {
		return List.of(
			(b) -> b,
			(b) -> b.withMaxStringLength(100),
			(b) -> b.withRemoveWhitespace(true),
			(b) -> b.withMaxStringLength(100).withRemoveWhitespace(true),
			(b) -> b.withMaxSize(maxSize),
			(b) -> b.withMaxStringLength(100).withMaxSize(maxSize),
			(b) -> b.withRemoveWhitespace(true).withMaxSize(maxSize)
		);
	}

	@Test
	public void path() {
		assertFiltered(ANONYMIZED, INPUT, (b) -> b.withAnonymizePaths("/\\n"));
		assertFiltered(PRUNED, INPUT, (b) -> b.withPrunePaths("/\\n"));
	}

	@Test
	public void multiplePaths() {
		assertFiltered(ANONYMIZED, INPUT, (b) -> b.withAnonymizePaths("/\\n", "/x"));
		assertFiltered(PRUNED, INPUT, (b) -> b.withPrunePaths("/\\n", "/x"));
	}

	@Test
	public void wildcardAndPath() {
		String input = "{\"a\":{\"\\n\":\"secret\",\"\\\\n\":\"secret2\"}}";
		String expected = "{\"a\":{\"\\n\":\"secret\",\"\\\\n\":\"*\"}}";
		assertFiltered(expected, input, (b) -> b.withAnonymizePaths("/*/\\n", "/a/x"));
	}

	@Test
	public void key() {
		assertFiltered(ANONYMIZED, INPUT, (b) -> b.withAnonymizeKeys("\\n"));
		assertFiltered(PRUNED, INPUT, (b) -> b.withPruneKeys("\\n"));
	}

	@Test
	public void backslashAfterFirstCharacter() {
		// the first key is a followed by an escaped backspace, the second is a, backslash and b
		String input = "{\"a\\b\":\"secret\",\"a\\\\b\":\"secret2\"}";
		String expected = "{\"a\\b\":\"secret\",\"a\\\\b\":\"*\"}";

		assertFiltered(expected, input, (b) -> b.withAnonymizePaths("/a\\b"));
		assertFiltered(expected, input, (b) -> b.withAnonymizePaths("/a\\b", "/x"));
		assertFiltered(expected, input, (b) -> b.withAnonymizeKeys("a\\b"));
	}

	@Test
	public void unicodeEscapedBackslash() {
		String input = "{\"\\n\":\"secret\",\"\\u005Cn\":\"secret2\"}";
		String expected = "{\"\\n\":\"secret\",\"\\u005Cn\":\"*\"}";

		assertFiltered(expected, input, (b) -> b.withAnonymizePaths("/\\n"));
		assertFiltered(expected, input, (b) -> b.withAnonymizeKeys("\\n"));
	}

	@Test
	public void matchPath() {
		char[] attributeChars = "\\n".toCharArray();
		byte[] attributeBytes = "\\n".getBytes(StandardCharsets.UTF_8);

		// escaped newline
		char[] newline = "\\n".toCharArray();
		assertThat(AbstractPathJsonFilter.matchPath(newline, 0, newline.length, attributeChars)).isFalse();
		assertThat(AbstractPathJsonFilter.matchPath("\\n".getBytes(StandardCharsets.UTF_8), 0, 2, attributeBytes)).isFalse();

		// escaped backslash followed by n
		char[] backslash = "\\\\n".toCharArray();
		assertThat(AbstractPathJsonFilter.matchPath(backslash, 0, backslash.length, attributeChars)).isTrue();
		assertThat(AbstractPathJsonFilter.matchPath("\\\\n".getBytes(StandardCharsets.UTF_8), 0, 3, attributeBytes)).isTrue();

		// plain keys are not affected
		char[] key = "key".toCharArray();
		assertThat(AbstractPathJsonFilter.matchPath(key, 0, key.length, key)).isTrue();
		assertThat(AbstractPathJsonFilter.matchPath("key".getBytes(StandardCharsets.UTF_8), 0, 3, "key".getBytes(StandardCharsets.UTF_8))).isTrue();
	}

	private static void assertFiltered(String expected, String input, UnaryOperator<DefaultJsonLogFilterBuilder> configuration) {
		// max size below the input size, so that max size filtering is active
		for(UnaryOperator<DefaultJsonLogFilterBuilder> variant : variants(input.length() - 1)) {
			JsonFilter filter = variant.apply(configuration.apply(DefaultJsonLogFilterBuilder.createInstance())).build();

			assertWithMessage(filter.toString()).that(filter.process(input)).isEqualTo(expected);
			assertWithMessage(filter.toString()).that(new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).isEqualTo(expected);
		}
	}
}
