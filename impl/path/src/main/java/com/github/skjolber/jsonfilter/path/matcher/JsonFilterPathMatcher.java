package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface JsonFilterPathMatcher {

	public boolean matches(String path);
	
	public JsonFilter getFilter();
}
