package com.github.skjolber.jsonfilter.spring.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class AllJsonFilterPathMatcher implements JsonFilterPathMatcher {

	private final JsonFilter jsonFilter;
	
	public AllJsonFilterPathMatcher(JsonFilter jsonFilter) {
		super();
		this.jsonFilter = jsonFilter;
	}

	@Override
	public boolean matches(String path) {
		return true;
	}

	@Override
	public JsonFilter getFilter() {
		return jsonFilter;
	}

}
