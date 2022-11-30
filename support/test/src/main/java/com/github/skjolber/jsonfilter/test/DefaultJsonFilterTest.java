package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.Arrays;

import com.github.skjolber.jsonfilter.test.directory.JsonFilterPropertiesFactory;

/**
 * 
 * Default test class.
 *
 */

public class DefaultJsonFilterTest extends AbstractJsonFilterTest implements JsonFilterConstants {

	public DefaultJsonFilterTest(boolean literal, boolean includePrettyPrinting) throws Exception {
		this(literal, 2, includePrettyPrinting);
	}

	public DefaultJsonFilterTest(boolean literal, int n, boolean includePrettyPrinting) throws Exception {
		super(new JsonFilterRunner(Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH), new File(toLevels(n) + BASE_PATH), new JsonFilterPropertiesFactory(Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH)), literal));
	}

	public DefaultJsonFilterTest() throws Exception {
		this(true, true);
	}

	public static String toLevels(int n) {
		StringBuilder builder = new StringBuilder();
		builder.append("./");
		
		for(int i = 0; i < n; i++) {
			builder.append("../");
		}
		
		return builder.toString();
	}
	
	
}
