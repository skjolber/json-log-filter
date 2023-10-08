package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.springframework.util.AntPathMatcher;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.AbstractJsonFilterPathMatcher;

public class AntJsonFilterPathMatcher extends AbstractJsonFilterPathMatcher {

	private final AntPathMatcher matcher;
	private final String pattern;

	public AntJsonFilterPathMatcher(AntPathMatcher matcher, String pattern, JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		super(validatingFilter, validatingMaxSizeFilter, nonvalidatingFilter, nonvalidatingMaxSizeFilter, maxSize);
		this.matcher = matcher;
		this.pattern = pattern;
	}

	@Override
	public boolean matches(String path) {
		return matcher.match(pattern, path);
	}

}
