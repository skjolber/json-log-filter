package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class EndPathItem implements PathItem {

	private final int index;
	private final PathItem previous;
	public final FilterType filterType;

	public EndPathItem(int index, PathItem previous, FilterType filterType) {
		super();
		this.index = index;
		this.previous = previous;
		this.filterType = filterType;
	}
	
	@Override
	public PathItem matchPath(String fieldName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PathItem constrain(int level) {
		if(index >= level) {
			return this;
		}
		return previous;
	}

	@Override
	public FilterType getType() {
		return filterType;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

}
