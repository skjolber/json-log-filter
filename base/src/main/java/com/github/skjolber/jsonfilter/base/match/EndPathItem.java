package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class EndPathItem implements PathItem {

	private final int index;
	private final PathItem parent;
	public final FilterType filterType;

	public EndPathItem(int index, PathItem previous, FilterType filterType) {
		super();
		this.index = index;
		this.parent = previous;
		this.filterType = filterType;
	}
	
	@Override
	public PathItem matchPath(String fieldName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PathItem constrain(int level) {
		if(index <= level) {
			return this;
		}
		return parent.constrain(level);
	}

	@Override
	public FilterType getType() {
		return filterType;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "EndPathItem [index=" + index + ", previous=" + parent + ", filterType=" + filterType + "]";
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
	public PathItem getParent() {
		return parent;
	}

}
