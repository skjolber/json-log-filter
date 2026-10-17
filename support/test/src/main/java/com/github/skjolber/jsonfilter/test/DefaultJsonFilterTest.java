package com.github.skjolber.jsonfilter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;

/**
 * 
 * Default test class.
 *
 */

public class DefaultJsonFilterTest extends AbstractJsonFilterDirectoryUnitTest implements JsonFilterConstants {
	
	private static List<String> NULLABLE = Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH, VALIDATING);

	/** Invalid JSON with whitespace, whose last string is not terminated, so that filters fail only at the end of the input. */
	protected static final String UNTERMINATED_LAST_STRING = "{ \"a\" : \"b\" , \"c\" : [ 1 , 2 ] , \"d\" : \"eeeeeeeeeeeeeeeeeeee";

	private static final String OUTPUT_PREFIX = "PREFIX|";
	
	public DefaultJsonFilterTest() throws Exception {
		this(true);
	}
	public DefaultJsonFilterTest(boolean literal, boolean whitespace, boolean unicode) throws Exception {
		this(literal, whitespace, unicode, false);
	}
	
	public DefaultJsonFilterTest(boolean literal, boolean whitespace, boolean unicode, boolean jsonTestSuite) throws Exception {
		super(new JsonFilterDirectoryUnitTestCollectionRunner(NULLABLE, literal, unicode, JsonFileCache.getInstance()), jsonTestSuite ? JsonTestSuiteRunner.fromResource("/jsonTestSuite/test_parsing",
				"/jsonTestSuite/test_transform") : null);
	}
	
	public DefaultJsonFilterTest(boolean literal) throws Exception {
		this(literal, true, true, true);
	}

	/**
	 * Assert that the filter fails on the input and leaves the output untouched: processing chars to a
	 * {@link StringBuilder} and bytes to a {@link ResizableByteArrayOutputStream}, each already holding a prefix,
	 * returns false and leaves only the prefix.
	 *
	 * @param filter the filter under test
	 * @param input input on which the filter fails
	 */

	protected static void assertExceptionLeavesOutputUntouched(JsonFilter filter, String input) {
		assertExceptionLeavesOutputUntouched(filter, input, null);
	}

	/**
	 * As {@link #assertExceptionLeavesOutputUntouched(JsonFilter, String)}, but passing metrics to the filter,
	 * for example metrics which throw so that the filter fails after writing its output.
	 *
	 * @param filter the filter under test
	 * @param input input on which the filter fails
	 * @param metrics metrics passed to the filter, or null to process without metrics
	 */

	protected static void assertExceptionLeavesOutputUntouched(JsonFilter filter, String input, JsonFilterMetrics metrics) {
		char[] chars = input.toCharArray();
		StringBuilder builder = new StringBuilder(OUTPUT_PREFIX);
		if(metrics == null) {
			assertFalse(filter.process(chars, 0, chars.length, builder));
		} else {
			assertFalse(filter.process(chars, 0, chars.length, builder, metrics));
		}
		assertEquals(OUTPUT_PREFIX, builder.toString());

		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		output.write(OUTPUT_PREFIX.getBytes(StandardCharsets.UTF_8));
		if(metrics == null) {
			assertFalse(filter.process(bytes, 0, bytes.length, output));
		} else {
			assertFalse(filter.process(bytes, 0, bytes.length, output, metrics));
		}
		assertEquals(OUTPUT_PREFIX, output.toString(StandardCharsets.UTF_8));
	}
	
}
