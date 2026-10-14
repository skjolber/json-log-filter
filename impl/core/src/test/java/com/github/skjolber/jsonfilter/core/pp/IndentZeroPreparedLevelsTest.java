package com.github.skjolber.jsonfilter.core.pp;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * Zero prepared levels is allowed, and then each level is indented without a cache.
 */

public class IndentZeroPreparedLevelsTest {

	@Test
	public void append() {
		assertSameIndentation(Indent.newBuilder().withPreparedLevels(0).build(), Indent.newBuilder().build());
		assertSameIndentation(Indent.newBuilder().withPreparedLevels(0).withTab().withWindowsLinebreak().build(), Indent.newBuilder().withTab().withWindowsLinebreak().build());
		// non-ascii indent character, multiple bytes in UTF-8
		assertSameIndentation(Indent.newBuilder().withPreparedLevels(0).withCharacter(' ').withCount(3).build(), Indent.newBuilder().withCharacter(' ').withCount(3).build());
	}

	@Test
	public void appendBeyondPreparedLevels() {
		assertSameIndentation(Indent.newBuilder().withPreparedLevels(1).build(), Indent.newBuilder().build());
		assertSameIndentation(Indent.newBuilder().withPreparedLevels(3).build(), Indent.newBuilder().build());
	}

	@Test
	public void prettyPrint() {
		String input = "{\"a\":{\"b\":[1,{\"c\":\"d\"}]}}";

		PrettyPrintingJsonFilter expected = new PrettyPrintingJsonFilter(Indent.newBuilder().build());
		PrettyPrintingJsonFilter filter = new PrettyPrintingJsonFilter(Indent.newBuilder().withPreparedLevels(0).build());

		assertThat(filter.process(input)).isNotNull();
		assertThat(filter.process(input)).isEqualTo(expected.process(input));
		assertThat(filter.process(input.getBytes(StandardCharsets.UTF_8))).isEqualTo(expected.process(input.getBytes(StandardCharsets.UTF_8)));
	}

	private static void assertSameIndentation(Indent indent, Indent expected) {
		for(int level = 0; level < 10; level++) {
			StringBuilder expectedChars = new StringBuilder();
			expected.append(expectedChars, level);
			StringBuilder chars = new StringBuilder();
			indent.append(chars, level);
			assertThat(chars.toString()).isEqualTo(expectedChars.toString());

			ResizableByteArrayOutputStream expectedBytes = new ResizableByteArrayOutputStream(128);
			expected.append(expectedBytes, level);
			ResizableByteArrayOutputStream bytes = new ResizableByteArrayOutputStream(128);
			indent.append(bytes, level);
			assertThat(bytes.toByteArray()).isEqualTo(expectedBytes.toByteArray());
			assertThat(new String(bytes.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(expectedChars.toString());
		}
	}
}
