package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.matcher.AllJsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcher;
import com.github.skjolber.jsonfilter.path.matcher.JsonFilterPathMatcherFactory;
import com.github.skjolber.jsonfilter.path.matcher.PrefixJsonFilterPathMatcher;

import org.springframework.util.AntPathMatcher;

public class JsonFilterAntPathMatcherFactory implements JsonFilterPathMatcherFactory {

	protected AntPathMatcher matcher = new AntPathMatcher();
	
	@Override
	public JsonFilterPathMatcher createMatcher(String antMatcher, JsonFilter validatingFilter, JsonFilter nonvalidatingFilter) {
		JsonFilterPathMatcher m; 
		if(antMatcher == null || antMatcher.isEmpty()) {
			m = new AllJsonFilterPathMatcher(validatingFilter, nonvalidatingFilter);
		} else if(!matcher.isPattern(antMatcher)) {
			m = new PrefixJsonFilterPathMatcher(antMatcher, validatingFilter, nonvalidatingFilter);
		} else {
			m = new AntJsonFilterPathMatcher(matcher, antMatcher, validatingFilter, nonvalidatingFilter);
		}
		return m;
	}
}