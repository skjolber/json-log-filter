package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyPathItem implements PathItem {

	private final int index;
	private final PathItem previous;
	private PathItem next;

	public AnyPathItem(int index, PathItem previous) {
		super();
		this.index = index;
		this.previous = previous;
	}
	
	public void setNext(PathItem next) {
		this.next = next;
	}

	@Override
	public PathItem matchPath(String fieldName) {
		return next;
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
		return null;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

}
