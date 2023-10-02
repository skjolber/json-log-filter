package com.github.skjolber.jsonfilter.base.path;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public abstract class PathItem {

	// from one and upwards
	protected final int level;
	protected final PathItem parent;
	
	public PathItem(int level, PathItem parent) {
		this.level = level;
		this.parent = parent;
	}

	public abstract PathItem matchPath(int level, String fieldName);
	public abstract PathItem matchPath(int level, final char[] source, int start, int end);
	public abstract PathItem matchPath(int level, final byte[] source, int start, int end);
	
	public FilterType getType() {
		return null;
	}
	
	public boolean hasType() {
		return false;
	}

	public PathItem constrain(int level) {
		if(this.level <= level) {
			return this;
		}
		return parent.constrain(level);
	}

	public int getLevel() {
		return level;
	}

}
