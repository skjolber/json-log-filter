package com.github.skjolber.jsonfilter.path.matcher;

import java.util.function.Predicate;

import com.github.skjolber.jsonfilter.JsonFilter;

public class DefaultJsonFilterPathMatcher implements JsonFilterPathMatcher {

	private final JsonFilter filter;
	private final Predicate<String> matcher;
	
	public DefaultJsonFilterPathMatcher(Predicate<String> matcher, JsonFilter filter) {
		this.matcher = matcher;
		this.filter = filter;
	}
	
	@Override
	public boolean matches(String path) {
		return matcher.test(path);
	}
	
	@Override
	public JsonFilter getFilter() {
		return filter;
	}
	
}
