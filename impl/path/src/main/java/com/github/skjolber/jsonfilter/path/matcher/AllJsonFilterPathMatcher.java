package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class AllJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	public AllJsonFilterPathMatcher(JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		super(validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
	}

	@Override
	public boolean matches(String path) {
		return true;
	}

}
