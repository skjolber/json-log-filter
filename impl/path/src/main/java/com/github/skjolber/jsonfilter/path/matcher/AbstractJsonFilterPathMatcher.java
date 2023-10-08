package com.github.skjolber.jsonfilter.path.matcher;

import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractJsonFilterPathMatcher implements JsonFilterPathMatcher {

	protected final JsonFilter validatingFilter;
	protected final JsonFilter validatingMaxSizeFilter;
	
	protected final JsonFilter nonvalidatingFilter;
	protected final JsonFilter nonvalidatingMaxSizeFilter;
	
	protected final int maxSize;
	
	public AbstractJsonFilterPathMatcher(JsonFilter validatingFilter, JsonFilter validatingMaxSizeFilter, JsonFilter nonvalidatingFilter, JsonFilter nonvalidatingMaxSizeFilter, int maxSize) {
		this.validatingFilter = validatingFilter;
		this.validatingMaxSizeFilter = validatingMaxSizeFilter;
		this.nonvalidatingFilter = nonvalidatingFilter;
		this.nonvalidatingMaxSizeFilter = nonvalidatingMaxSizeFilter;
		this.maxSize = maxSize;
	}
	
	@Override
	public JsonFilter getFilter(boolean validate, int size) {
		if(validate) {
			if(size > maxSize) {
				return validatingMaxSizeFilter;
			}
			return validatingFilter;
		}
		if(size > maxSize) {
			return nonvalidatingMaxSizeFilter;
		}
		return nonvalidatingFilter;
	}
	
}
