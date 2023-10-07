package com.github.skjolber.jsonfilter.base.path;

public class StarPathItem extends PathItem {

	private PathItem next;

	public StarPathItem(int level, PathItem parent) {
		super(level, parent);
	}
	
	public void setNext(PathItem next) {
		this.next = next;
	}

	@Override
	public PathItem matchPath(int level, String fieldName) {
		return next;
	}

	@Override
	public PathItem matchPath(int level, char[] source, int start, int end) {
		return next;
	}

	@Override
	public PathItem matchPath(int level, byte[] source, int start, int end) {
		return next;
	}

}
