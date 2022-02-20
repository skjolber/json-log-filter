package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class PrefixJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	private final String prefix;
	
	public PrefixJsonFilterPathMatcher(String prefix, JsonFilter filterWithValidate, JsonFilter filter) {
		super(filterWithValidate, filter);
		this.prefix = prefix;
	}

	@Override
	public boolean matches(String path) {
		return path.startsWith(prefix);
	}


}
