package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface JsonFilterPathMatcherFactory {

	public JsonFilterPathMatcher createMatcher(String matcher, JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize);
}
