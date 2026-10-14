package com.github.skjolber.jsonfilter.jackson;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

/**
 * The Jackson filters are validating, so they must reject input with more than one root value.
 */

public class JacksonSingleRootValueJsonFilterTest {

	// the first root value is short, so that also max size filters read the start of the second root value
	private static final String[] MULTIPLE_ROOT_VALUES = new String[] {
		"{\"a\":1}{\"injected\":true}",
		"{\"a\":1} {\"injected\":true}",
		"{\"a\":1}\n{\"injected\":true}\n",
		"{\"a\":1}123456789",
		"[1][2]",
		"1 2 3",
		"\"x\" \"y\"",
		"null true"
	};

	private static final String[] SINGLE_ROOT_VALUE = new String[] {
		"{\"b\":1}",
		"{\"b\":1} \n",
		"[1,2]",
		"1",
		"\"x\""
	};

	private static final String PREFIX = "PREFIX|";

	private static List<JsonFilter> filters(int maxSize) {
		List<IntFunction<JsonFilter>> factories = List.of(
			(size) -> JacksonJsonLogFilterBuilder.createInstance().build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withRemoveWhitespace(true).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxStringLength(100).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxSize(size).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxSize(size).withRemoveWhitespace(true).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxStringLength(100).withMaxSize(size).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a").build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizeKeys("a").build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.a").withMaxSize(size).build()
		);
		List<JsonFilter> filters = new ArrayList<>();
		for(IntFunction<JsonFilter> factory : factories) {
			filters.add(factory.apply(maxSize));
		}
		return filters;
	}

	@Test
	public void coversAllFilters() {
		Set<Class<?>> classes = new HashSet<>();
		for(JsonFilter filter : filters(10)) {
			classes.add(filter.getClass());
		}
		assertThat(classes).containsExactly(
			DefaultJacksonJsonFilter.class,
			JacksonMaxStringLengthJsonFilter.class,
			JacksonMaxSizeJsonFilter.class,
			JacksonMaxSizeMaxStringLengthJsonFilter.class,
			JacksonPathMaxStringLengthJsonFilter.class,
			JacksonAnyPathMaxStringLengthJsonFilter.class,
			JacksonPathMaxSizeMaxStringLengthJsonFilter.class
		);
	}

	@Test
	public void multipleRootValuesAreRejected() {
		for(String input : MULTIPLE_ROOT_VALUES) {
			// max size above the input size, so that max size filters pass the input through as-is
			assertRejected(input, input.length() + 1, "");
			// max size below the input size, so that max size filters write the output token by token
			assertRejected(input, input.length() - 1, "");
		}
	}

	@Test
	public void outputIsUntouchedWhenRejected() {
		// the second root value starts well below the max size, also counting the existing output
		String input = "{\"a\":1}{\"injected\":true}";

		assertRejected(input, input.length() + 1, PREFIX);
		assertRejected(input, input.length() - 1, PREFIX);
	}

	@Test
	public void singleRootValueIsAccepted() {
		for(String input : SINGLE_ROOT_VALUE) {
			for(JsonFilter filter : filters(input.length() + 1)) {
				String name = filter.getClass().getSimpleName() + " " + input;

				assertWithMessage(name).that(filter.process(input)).isNotNull();
				assertWithMessage(name).that(filter.process(input.getBytes(StandardCharsets.UTF_8))).isNotNull();
			}
		}
	}

	@Test
	public void maxSizeReachedWithinFirstRootValue() {
		// the rest of the input is not read once the max size is reached
		String input = "{\"a\":\"xxxxxxxxxxxxxxxxxxxx\",\"b\":\"yyyyyyyyyyyyyyyyyyyy\"}{\"injected\":true}";

		for(JsonFilter filter : filters(30)) {
			if(((AbstractJsonFilter)filter).getMaxSize() == Integer.MAX_VALUE) {
				continue;
			}
			String name = filter.getClass().getSimpleName();

			String fromChars = filter.process(input);
			assertWithMessage(name).that(fromChars).isNotNull();
			assertWithMessage(name).that(fromChars).doesNotContain("injected");

			byte[] fromBytes = filter.process(input.getBytes(StandardCharsets.UTF_8));
			assertWithMessage(name).that(fromBytes).isNotNull();
			assertWithMessage(name).that(new String(fromBytes, StandardCharsets.UTF_8)).doesNotContain("injected");
		}
	}

	private static void assertRejected(String input, int maxSize, String existing) {
		for(JsonFilter filter : filters(maxSize)) {
			String name = filter.getClass().getSimpleName() + " " + input;

			assertWithMessage(name).that(filter.process(input)).isNull();
			assertWithMessage(name).that(filter.process(input.toCharArray())).isNull();

			StringBuilder fromChars = new StringBuilder(existing);
			assertWithMessage(name).that(filter.process(input.toCharArray(), 0, input.length(), fromChars)).isFalse();
			assertWithMessage(name).that(fromChars.toString()).isEqualTo(existing);

			byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
			assertWithMessage(name).that(filter.process(bytes)).isNull();

			byte[] prefix = existing.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream fromBytes = new ResizableByteArrayOutputStream(128);
			fromBytes.write(prefix);
			assertWithMessage(name).that(filter.process(bytes, 0, bytes.length, fromBytes)).isFalse();
			assertWithMessage(name).that(fromBytes.toByteArray()).isEqualTo(prefix);

			StringBuilder fromBytesToChars = new StringBuilder(existing);
			if(filter instanceof JacksonJsonFilter) {
				assertWithMessage(name).that(((JacksonJsonFilter)filter).process(bytes, 0, bytes.length, fromBytesToChars)).isFalse();
			} else {
				assertWithMessage(name).that(((DefaultJacksonJsonFilter)filter).process(bytes, 0, bytes.length, fromBytesToChars, null)).isFalse();
			}
			assertWithMessage(name).that(fromBytesToChars.toString()).isEqualTo(existing);
		}
	}
}
