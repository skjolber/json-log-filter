package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class DefaultJsonFilterPathMatcherFactory implements JsonFilterPathMatcherFactory {

	@Override
	public JsonFilterPathMatcher createMatcher(String matcher, JsonFilter filterWithValidate, JsonFilter filter) {
		JsonFilterPathMatcher m; 
		if(matcher == null || matcher.isEmpty()) {
			m = new AllJsonFilterPathMatcher(filterWithValidate, filter);
		} else {
			m = new PrefixJsonFilterPathMatcher(matcher, filterWithValidate, filter);
		}
		return m;
	}

}
