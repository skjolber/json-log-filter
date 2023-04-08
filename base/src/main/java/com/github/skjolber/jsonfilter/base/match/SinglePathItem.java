package com.github.skjolber.jsonfilter.base.match;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SinglePathItem implements PathItem {

	private final int index;
	private final PathItem previous;
	private final String fieldName;
	private PathItem next;

	public SinglePathItem(int index, String fieldName, PathItem previous) {
		super();
		this.index = index;
		this.fieldName = fieldName;
		this.previous = previous;
	}
	
	public void setNext(PathItem next) {
		this.next = next;
	}

	@Override
	public PathItem matchPath(String fieldName) {
		if(this.fieldName.equals(fieldName)) {
			return next;
		}
		return this;
	}

	@Override
	public PathItem constrain(int level) {
		if(index >= level) {
			return this;
		}
		return previous;
	}
	
	public boolean hasNext() {
		return next != null;
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
