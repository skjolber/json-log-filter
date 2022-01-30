package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface JsonFilterPathMatcherFactory {

	public JsonFilterPathMatcher createMatcher(String matcher, JsonFilter jsonFilter);
}
