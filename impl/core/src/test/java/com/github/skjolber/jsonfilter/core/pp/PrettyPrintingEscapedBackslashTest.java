package com.github.skjolber.jsonfilter.core.pp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

/**
 * A string which ends with an escaped backslash, like "x\\", must end at its closing quote.
 */
public class PrettyPrintingEscapedBackslashTest {

	private final PrettyPrintingJsonFilter filter = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

	private final ObjectMapper mapper = new ObjectMapper();

	private void assertPrettyPrinted(String input, String expected) {
		assertWithMessage("char[] " + input).that(filter.process(input)).isEqualTo(expected);

		byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
		assertWithMessage("byte[] " + input).that(bytes).isNotNull();
		assertWithMessage("byte[] " + input).that(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(expected);
	}

	@Test
	public void testObjectValueEndingWithEscapedBackslash() {
		assertPrettyPrinted("{\"a\":\"x\\\\\",\"b\":1}", "{\n  \"a\": \"x\\\\\",\n  \"b\": 1\n}");
	}

	@Test
	public void testArrayValueEndingWithEscapedBackslash() {
		// the space inside the second string must be kept
		assertPrettyPrinted("[\"a\\\\\",\"b c\\\"d\"]", "[\n  \"a\\\\\",\n  \"b c\\\"d\"\n]");
	}

	@Test
	public void testKeyEndingWithEscapedBackslash() {
		assertPrettyPrinted("{\"a\\\\\":\"b c\"}", "{\n  \"a\\\\\": \"b c\"\n}");
	}

	@Test
	public void testValueEndingWithEscapedBackslashAndQuote() {
		// escaped backslash followed by an escaped quote: the string does not end at the second quote
		assertPrettyPrinted("[\"a\\\\\\\"\",\"b c\"]", "[\n  \"a\\\\\\\"\",\n  \"b c\"\n]");
	}

	@Test
	public void testSlashSequences() {
		for(int slashes = 0; slashes <= 6; slashes += 2) {
			// an even number of slashes at the end of each string
			String value = "x" + "\\".repeat(slashes);
			String input = "{\"k\":\"" + value + "\",\"a b\":[\"" + value + "\",\"c d\",{\"" + value + "\":\"" + value + "\"}],\"e\":\"f g\"}";

			String chars = filter.process(input);
			assertWithMessage(input).that(chars).isNotNull();
			assertWithMessage(input).that(mapper.readTree(chars)).isEqualTo(mapper.readTree(input));

			byte[] bytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
			assertWithMessage(input).that(bytes).isNotNull();
			assertWithMessage(input).that(mapper.readTree(bytes)).isEqualTo(mapper.readTree(input));

			assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(chars);
		}
	}
}
