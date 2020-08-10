package com.github.skjolber.jsonfilter.spring.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class PrefixJsonFilterPathMatcher implements JsonFilterPathMatcher {

	private final String prefix;
	private final JsonFilter filter;
	
	public PrefixJsonFilterPathMatcher(String prefix, JsonFilter filter) {
		this.prefix = prefix;
		this.filter = filter;
	}

	@Override
	public boolean matches(String path) {
		return path.startsWith(prefix);
	}

	@Override
	public JsonFilter getFilter() {
		return filter;
	}

}
