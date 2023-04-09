package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public interface PathItem {

	PathItem matchPath(String fieldName);

	PathItem matchPath(final char[] source, int start, int end);
	PathItem matchPath(final byte[] source, int start, int end);
	
	PathItem constrain(int level);
	
	FilterType getType();
	
	boolean hasType();
	
	int getIndex();

	PathItem getParent();

}
