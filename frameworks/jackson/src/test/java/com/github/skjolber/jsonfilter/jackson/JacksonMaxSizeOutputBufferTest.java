package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * Checks that the Jackson max-size filters only count what they write themselves against the max size,
 * not content already in the output buffer, and that a root scalar is never dropped.
 */

public class JacksonMaxSizeOutputBufferTest {

	private static final String INPUT = "{\"a\":\"hello\",\"b\":[1,2,3],\"c\":true}";
	private static final String EXPECTED = "{\"a\":\"hello\",\"b\":[1,2,3]}";

	private static final String PREFIX = "P".repeat(100);

	private static List<JsonFilter> filters(int maxSize) {
		return List.of(
			JacksonJsonLogFilterBuilder.newBuilder().withMaxSize(maxSize).build(),
			JacksonJsonLogFilterBuilder.newBuilder().withMaxStringLength(100).withMaxSize(maxSize).build(),
			JacksonJsonLogFilterBuilder.newBuilder().withAnonymizePaths("$.secret").withMaxSize(maxSize).build()
		);
	}

	@Test
	public void maxSizeIgnoresExistingStringBuilderContent() {
		for(JsonFilter filter : filters(30)) {
			char[] chars = INPUT.toCharArray();
			StringBuilder output = new StringBuilder(PREFIX);
			assertWithMessage(filter.getClass().getName()).that(filter.process(chars, 0, chars.length, output)).isTrue();
			assertWithMessage(filter.getClass().getName()).that(output.toString()).isEqualTo(PREFIX + EXPECTED);

			byte[] bytes = INPUT.getBytes(StandardCharsets.UTF_8);
			output = new StringBuilder(PREFIX);
			assertWithMessage(filter.getClass().getName()).that(((JacksonJsonFilter)filter).process(bytes, 0, bytes.length, output)).isTrue();
			assertWithMessage(filter.getClass().getName()).that(output.toString()).isEqualTo(PREFIX + EXPECTED);
		}
	}

	@Test
	public void maxSizeIgnoresExistingOutputStreamContent() {
		byte[] prefix = PREFIX.getBytes(StandardCharsets.UTF_8);
		for(JsonFilter filter : filters(30)) {
			byte[] bytes = INPUT.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(256);
			output.write(prefix, 0, prefix.length);
			assertWithMessage(filter.getClass().getName()).that(filter.process(bytes, 0, bytes.length, output)).isTrue();
			assertWithMessage(filter.getClass().getName()).that(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(PREFIX + EXPECTED);
		}
	}

	@Test
	public void rootScalarIsKept() {
		// consistent with the core max size filter, which does not cut a root string
		String scalar = "\"" + "s".repeat(28) + "\"";
		for(JsonFilter filter : filters(10)) {
			assertWithMessage(filter.getClass().getName()).that(filter.process(scalar)).isEqualTo(scalar);
			assertWithMessage(filter.getClass().getName()).that(new String(filter.process(scalar.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).isEqualTo(scalar);
		}

		JsonFilter maxStringLength = JacksonJsonLogFilterBuilder.newBuilder().withMaxStringLength(5).withMaxSize(10).build();
		assertThat(maxStringLength.process(scalar)).isEqualTo("\"sssss... + 23\"");
	}
}
