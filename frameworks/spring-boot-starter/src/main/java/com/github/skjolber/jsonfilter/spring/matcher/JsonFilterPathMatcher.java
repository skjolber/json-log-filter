package com.github.skjolber.jsonfilter.spring.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface JsonFilterPathMatcher {

	public boolean matches(String path);
	
	public JsonFilter getFilter();
}
