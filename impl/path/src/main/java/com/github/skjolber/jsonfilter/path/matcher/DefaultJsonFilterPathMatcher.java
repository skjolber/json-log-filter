package com.github.skjolber.jsonfilter.path.matcher;

import java.util.function.Predicate;

import com.github.skjolber.jsonfilter.JsonFilter;

public class DefaultJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	protected final Predicate<String> matcher;
	
	public DefaultJsonFilterPathMatcher(Predicate<String> matcher, JsonFilter filterWithValidate, JsonFilter filter) {
		super(filterWithValidate, filter);
		this.matcher = matcher;
	}
	
	@Override
	public boolean matches(String path) {
		return matcher.test(path);
	}
	
}
