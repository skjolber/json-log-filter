package com.github.skjolber.jsonfilter.test;

import java.io.File;
import java.util.Arrays;

/**
 * 
 * Default test class.
 *
 */

public class DefaultJsonFilterTest extends AbstractJsonFilterTest implements JsonFilterConstants {

	public DefaultJsonFilterTest(boolean literal) throws Exception {
		super(new JsonFilterRunner(Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH), new File(BASE_PATH), new JsonFilterPropertiesFactory(Arrays.asList(PASSTHROUGH_XPATH, ANY_PASSTHROUGH_XPATH)), literal));
	}

	public DefaultJsonFilterTest() throws Exception {
		this(true);
	}

}
