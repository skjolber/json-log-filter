package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * Checks that the token-copying Jackson filters write numbers exactly as in the input,
 * and leave the output buffer untouched when they return false.
 */

public class JacksonJsonFilterNumbersAndFailureTest {

	private static final String NUMBERS = "{\"amount\": 12345678901234567890.12, \"big\": 1e400, \"tiny\": 1e-400, \"x\": 1.10, \"e\": 1E+2, \"neg\": -0, "
			+ "\"int\": 123456789012345678901234567890, \"array\": [1.0, -2.50e-3]}";
	private static final String NUMBERS_COMPACT = NUMBERS.replace(" ", "");

	private static final String INVALID = "{\"a\": \"xxxxxxxxxxxxxxxxxxxx\", \"b\": [1, 2, 3], \"c\": tru}";

	private static final String PREFIX = "PREFIX|";

	private static List<JsonFilter> filters(int maxSize) {
		// each builder combination yields one of the token-copying filters
		List<IntFunction<JsonFilter>> factories = List.of(
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxStringLength(100).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxSize(size).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withMaxStringLength(100).withMaxSize(size).build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.secret").build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizeKeys("secret").build(),
			(size) -> JacksonJsonLogFilterBuilder.createInstance().withAnonymizePaths("$.secret").withMaxSize(size).build()
		);
		List<JsonFilter> filters = new ArrayList<>();
		for(IntFunction<JsonFilter> factory : factories) {
			filters.add(factory.apply(maxSize));
		}
		return filters;
	}

	@Test
	public void coversAllTokenCopyingFilters() {
		Set<Class<?>> classes = new HashSet<>();
		for(JsonFilter filter : filters(NUMBERS.length() - 1)) {
			classes.add(filter.getClass());
		}
		assertEquals(Set.of(
			JacksonMaxStringLengthJsonFilter.class,
			JacksonMaxSizeJsonFilter.class,
			JacksonMaxSizeMaxStringLengthJsonFilter.class,
			JacksonPathMaxStringLengthJsonFilter.class,
			JacksonAnyPathMaxStringLengthJsonFilter.class,
			JacksonPathMaxSizeMaxStringLengthJsonFilter.class
		), classes);
	}

	@Test
	public void numbersAreCopiedExactly() {
		// max size below the input size, so that max size filters do not pass the input through;
		// whitespace removal makes the whole document fit
		for(JsonFilter filter : filters(NUMBERS.length() - 1)) {
			String name = filter.getClass().getSimpleName();

			StringBuilder fromChars = new StringBuilder();
			assertTrue(filter.process(NUMBERS.toCharArray(), 0, NUMBERS.length(), fromChars), name);
			assertEquals(NUMBERS_COMPACT, fromChars.toString(), name);

			byte[] bytes = NUMBERS.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream fromBytes = new ResizableByteArrayOutputStream(128);
			assertTrue(filter.process(bytes, 0, bytes.length, fromBytes), name);
			assertEquals(NUMBERS_COMPACT, fromBytes.toString(StandardCharsets.UTF_8), name);

			StringBuilder fromBytesToChars = new StringBuilder();
			assertTrue(((JacksonJsonFilter)filter).process(bytes, 0, bytes.length, fromBytesToChars), name);
			assertEquals(NUMBERS_COMPACT, fromBytesToChars.toString(), name);
		}
	}

	@Test
	public void outputIsUnaffectedOnFailure() {
		for(JsonFilter filter : filters(INVALID.length() - 1)) {
			String name = filter.getClass().getSimpleName();

			StringBuilder fromChars = new StringBuilder(PREFIX);
			assertFalse(filter.process(INVALID.toCharArray(), 0, INVALID.length(), fromChars), name);
			assertEquals(PREFIX, fromChars.toString(), name);

			byte[] bytes = INVALID.getBytes(StandardCharsets.UTF_8);
			byte[] prefix = PREFIX.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream fromBytes = new ResizableByteArrayOutputStream(128);
			fromBytes.write(prefix);
			assertFalse(filter.process(bytes, 0, bytes.length, fromBytes), name);
			assertArrayEquals(prefix, fromBytes.toByteArray(), name);

			StringBuilder fromBytesToChars = new StringBuilder(PREFIX);
			assertFalse(((JacksonJsonFilter)filter).process(bytes, 0, bytes.length, fromBytesToChars), name);
			assertEquals(PREFIX, fromBytesToChars.toString(), name);
		}
	}
}
