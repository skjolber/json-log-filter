package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.PathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceNewlineStringJsonFilter;

/**
 * Per JsonFilter: if processing fails, the output buffer is unaffected.
 */

public class FailureLeavesOutputUntouchedTest {

	// the last string is not terminated
	private static final String INVALID = "{ \"a\" : \"b\" , \"c\" : [ 1 , 2 ] , \"d\" : \"eeeeeeeeeeeeeeeeeeee";

	private static final String PREFIX = "PREFIX|";

	// max size below the input size, but large enough to reach the end once whitespace is removed
	private static final List<IntFunction<JsonFilter>> FILTERS = List.of(
		(maxSize) -> new RemoveWhitespaceJsonFilter(),
		(maxSize) -> new RemoveWhitespaceNewlineStringJsonFilter(),
		(maxSize) -> new MaxStringLengthRemoveWhitespaceJsonFilter(100),
		(maxSize) -> new MaxSizeRemoveWhitespaceJsonFilter(maxSize),
		(maxSize) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(100, maxSize),
		(maxSize) -> new PathMaxStringLengthRemoveWhitespaceJsonFilter(100, -1, new String[] {"$.z"}, null),
		(maxSize) -> new PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(100, maxSize, -1, new String[] {"$.z"}, null),
		(maxSize) -> new MaxSizeJsonFilter(maxSize),
		(maxSize) -> new MaxSizeFromEndJsonFilter(maxSize),
		(maxSize) -> new PrettyPrintingJsonFilter(Indent.newBuilder().build())
	);

	@Test
	public void outputIsUnaffectedOnFailure() {
		for(IntFunction<JsonFilter> factory : FILTERS) {
			JsonFilter filter = factory.apply(INVALID.length() - 1);
			String name = filter.getClass().getSimpleName();

			StringBuilder builder = new StringBuilder(PREFIX);
			assertFalse(filter.process(INVALID.toCharArray(), 0, INVALID.length(), builder), name);
			assertEquals(PREFIX, builder.toString(), name);

			byte[] prefix = PREFIX.getBytes(StandardCharsets.UTF_8);
			byte[] bytes = INVALID.getBytes(StandardCharsets.UTF_8);
			ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
			output.write(prefix);
			assertFalse(filter.process(bytes, 0, bytes.length, output), name);
			assertArrayEquals(prefix, output.toByteArray(), name);
		}
	}
}
