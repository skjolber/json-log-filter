package com.github.skjolber.jsonfilter.test;

public interface JsonFilterConstants {

	public static final String INVALID_PATH = "no/match";
	public static final String PASSTHROUGH_XPATH = "/no/match";
	public static final String ANY_PASSTHROUGH_XPATH = "//noMatch";

	public static final String DEFAULT_ANY_PATH = "//key";
	public static final String DEFAULT_PATH = "/key";
	public static final String DEFAULT_WILDCARD_PATH = "/*";

	public static final int DEFAULT_MAX_STRING_LENGTH = 5;
	public static final int DEFAULT_MAX_SIZE = 20;

	public static final String BASE_PATH = "support/test/src/main/resources/json/";

	public static final String DEEP_PATH1 = "/grandparent/parent/child1";
	public static final String DEEP_PATH2 = "/grandparent/parent/child2";
	public static final String DEEP_PATH3 = "/grandparent/parent/child3";

	public static final String TRUNCATED = "{\"unterminated\":\"string value";
	public static final char[] FULL = "{\"terminated\":\"string value\"}".toCharArray();
	public static final String INCORRECT_LEVEL = "{";

	public static final String VALIDATING = "validating";

}
