package com.github.skjolber.jsonfilter.base.path;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class EndPathItem extends PathItem {

	public final FilterType filterType;

	public EndPathItem(int level, PathItem parent, FilterType filterType) {
		super(level, parent);
		this.filterType = filterType;
	}

	@Override
	public FilterType getType() {
		return filterType;
	}
	
	@Override
	public String toString() {
		return "EndPathItem [level=" + level + ", previous=" + parent + ", filterType=" + filterType + "]";
	}

	@Override
	public boolean hasType() {
		return true;
	}

	@Override
	public PathItem matchPath(char[] source, int start, int end) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PathItem matchPath(byte[] source, int start, int end) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PathItem matchPath(String fieldName) {
		throw new UnsupportedOperationException();
	}

}
