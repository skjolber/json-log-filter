package com.github.skjolber.jsonfilter.test;

import java.util.Arrays;
import java.util.List;

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
	
	public DefaultJsonFilterTest(boolean literal) throws Exception {
		super(new JsonFilterRunner(NULLABLE, literal, JsonFileCache.getInstance()));
	}
	
}
