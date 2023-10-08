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
	public PathItem matchPath(int level, char[] source, int start, int end) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PathItem matchPath(int level, byte[] source, int start, int end) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PathItem matchPath(int level, String fieldName) {
		throw new UnsupportedOperationException();
	}

	public boolean hasType() {
		return true;
	}

}
