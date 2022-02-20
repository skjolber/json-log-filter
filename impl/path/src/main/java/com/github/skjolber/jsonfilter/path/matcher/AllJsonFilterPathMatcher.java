package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class AllJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	public AllJsonFilterPathMatcher(JsonFilter filterWithValidate, JsonFilter filter) {
		super(filterWithValidate, filter);
	}

	@Override
	public boolean matches(String path) {
		return true;
	}

}
