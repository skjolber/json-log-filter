package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class DefaultJsonFilterPathMatcherFactory implements JsonFilterPathMatcherFactory {

	@Override
	public JsonFilterPathMatcher createMatcher(String matcher, JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		JsonFilterPathMatcher m; 
		if(matcher == null || matcher.isEmpty()) {
			m = new AllJsonFilterPathMatcher(validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
		} else {
			m = new PrefixJsonFilterPathMatcher(matcher, validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
		}
		return m;
	}

}
