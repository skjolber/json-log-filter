package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractJsonFilterPathMatcher implements JsonFilterPathMatcher {

	protected final JsonFilter validatingFilter;
	protected final JsonFilter nonvalidatingFilter;
	
	public AbstractJsonFilterPathMatcher(JsonFilter validatingFilter, JsonFilter nonvalidatingFilter) {
		this.validatingFilter = validatingFilter;
		this.nonvalidatingFilter = nonvalidatingFilter;
	}
	
	@Override
	public JsonFilter getFilter(boolean validate) {
		if(validate) {
			return validatingFilter;
		}
		return nonvalidatingFilter;
	}
	
}
