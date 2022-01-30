package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class DefaultJsonFilterPathMatcherFactory implements JsonFilterPathMatcherFactory {

	@Override
	public JsonFilterPathMatcher createMatcher(String matcher, JsonFilter jsonFilter) {
		JsonFilterPathMatcher m; 
		if(matcher == null || matcher.isEmpty()) {
			m = new AllJsonFilterPathMatcher(jsonFilter);
		} else {
			m = new PrefixJsonFilterPathMatcher(matcher, jsonFilter);
		}
		return m;
	}

}
