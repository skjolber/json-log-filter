package com.github.skjolber.jsonfilter.test;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;

/**
 * 
 * Default test class.
 *
 */

public class DefaultJsonFilterTest extends AbstractJsonFilterDirectoryUnitTest implements JsonFilterConstants {
	
	private static List<String> NULLABLE = Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH, VALIDATING);

	
	public DefaultJsonFilterTest() throws Exception {
		this(true);
	}
	public DefaultJsonFilterTest(boolean literal, boolean whitespace, boolean unicode) throws Exception {
		this(literal, whitespace, unicode, false);
	}
	
	public DefaultJsonFilterTest(boolean literal, boolean whitespace, boolean unicode, boolean jsonTestSuite) throws Exception {
		super(new JsonFilterDirectoryUnitTestCollectionRunner(NULLABLE, literal, unicode, JsonFileCache.getInstance()), jsonTestSuite ? JsonTestSuiteRunner.fromResource("/jsonTestSuite/test_parsing", "/jsonTestSuite/test_transform") : null);
	}
	
	public DefaultJsonFilterTest(boolean literal) throws Exception {
		this(literal, true, true, true);
	}
	
}
