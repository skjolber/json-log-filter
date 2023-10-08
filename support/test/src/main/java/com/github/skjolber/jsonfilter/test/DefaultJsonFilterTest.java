package com.github.skjolber.jsonfilter.test;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.jsonfilter.test.cache.JsonFileCache;

/**
 * 
 * Default test class.
 *
 */

public class DefaultJsonFilterTest extends AbstractJsonFilterTest implements JsonFilterConstants {
	
	private static List<String> NULLABLE = Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH, VALIDATING);

	
	public DefaultJsonFilterTest() throws Exception {
		this(true);
	}
	public DefaultJsonFilterTest(boolean literal, boolean whitespace, boolean unicode) throws Exception {
		super(new JsonFilterRunner(NULLABLE, literal, whitespace, unicode, JsonFileCache.getInstance()));
	}
	
	public DefaultJsonFilterTest(boolean literal) throws Exception {
		this(literal, true, true);
	}
	
}
