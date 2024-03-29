package com.github.skjolber.jsonfilter.base.path;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class SinglePathItem extends PathItem {

	private final String fieldName;
	public final byte[] fieldNameBytes;
	public final char[] fieldNameChars;

	private PathItem next;

	public SinglePathItem(int level, String fieldName, PathItem parent) {
		super(level, parent);
		this.fieldName = fieldName;
		this.fieldNameBytes = fieldName.getBytes(StandardCharsets.UTF_8);
		this.fieldNameChars = fieldName.toCharArray();
	}
	
	public void setNext(PathItem next) {
		this.next = next;
	}

	@Override
	public PathItem matchPath(int level, String fieldName) {
		if(level == this.level && this.fieldName.equals(fieldName)) {
			return next;
		}
		return this;
	}

	@Override
	public PathItem matchPath(int level, char[] source, int start, int end) {
		if(level == this.level && AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars)) {
			return next;
		}
		
		return this;
	}

	@Override
	public PathItem matchPath(int level, byte[] source, int start, int end) {
		if(level == this.level && AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes)) {
			return next;
		}
		return this;
	}

}
