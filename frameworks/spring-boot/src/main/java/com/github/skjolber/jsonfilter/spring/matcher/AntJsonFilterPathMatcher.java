package com.github.skjolber.jsonfilter.spring.matcher;

import org.springframework.util.AntPathMatcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class AntJsonFilterPathMatcher implements JsonFilterPathMatcher {

	private final AntPathMatcher matcher;
	private final JsonFilter filter;
	private final String pattern;
	
	public AntJsonFilterPathMatcher(AntPathMatcher matcher, String pattern, JsonFilter filter) {
		this.matcher = matcher;
		this.filter = filter;
		this.pattern = pattern;
	}
	
	@Override
	public boolean matches(String path) {
		return matcher.match(pattern, path);
	}
	@Override
	public JsonFilter getFilter() {
		return filter;
	}
	
}
