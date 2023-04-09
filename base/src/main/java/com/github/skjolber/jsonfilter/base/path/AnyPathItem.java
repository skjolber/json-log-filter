package com.github.skjolber.jsonfilter.base.path;

public class AnyPathItem extends PathItem {

	private PathItem next;

	public AnyPathItem(int level, PathItem parent) {
		super(level, parent);
	}
	
	public void setNext(PathItem next) {
		this.next = next;
	}

	@Override
	public PathItem matchPath(String fieldName) {
		return next;
	}

	@Override
	public PathItem matchPath(char[] source, int start, int end) {
		return next;
	}

	@Override
	public PathItem matchPath(byte[] source, int start, int end) {
		return next;
	}

}
