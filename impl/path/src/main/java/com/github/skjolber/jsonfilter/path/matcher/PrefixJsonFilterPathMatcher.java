package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class PrefixJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	private final String prefix;
	
	public PrefixJsonFilterPathMatcher(String prefix, JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		super(validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
		this.prefix = prefix;
	}

	@Override
	public boolean matches(String path) {
		return path.startsWith(prefix);
	}


}
