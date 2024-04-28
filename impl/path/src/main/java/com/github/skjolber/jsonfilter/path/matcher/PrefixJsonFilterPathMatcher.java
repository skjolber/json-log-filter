package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public class PrefixJsonFilterPathMatcher extends DefaultJsonFilterPathMatcher {

	public PrefixJsonFilterPathMatcher(String prefix, JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		super( (p) -> p != null && p.startsWith(prefix), validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
	}

}
