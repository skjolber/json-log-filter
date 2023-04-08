package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public interface PathItem {

	PathItem matchPath(String fieldName);

//	public PathItem matchPath(final char[] source, int start, int end);
	
	PathItem constrain(int level);
	
	FilterType getType();
	
	int getIndex();
}
