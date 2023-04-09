package com.github.skjolber.jsonfilter.base.match;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SinglePathItem implements PathItem {

	private final int index;
	private final PathItem parent;
	
	private final String fieldName;
	public final byte[] fieldNameBytes;
	public final char[] fieldNameChars;

	private PathItem next;

	public SinglePathItem(int index, String fieldName, PathItem previous) {
		super();
		this.index = index;
		this.fieldName = fieldName;
		this.fieldNameBytes = fieldName.getBytes(StandardCharsets.UTF_8);
		this.fieldNameChars = fieldName.toCharArray();
		this.parent = previous;
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
		if(index <= level) {
			return this;
		}
		return parent.constrain(level);
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
	
	@Override
	public boolean hasType() {
		return false;
	}

	@Override
	public PathItem matchPath(char[] source, int start, int end) {
		if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars)) {
			return next;
		}
		
		return null;
	}

	@Override
	public PathItem matchPath(byte[] source, int start, int end) {
		if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes)) {
			return next;
		}
		return null;
	}

	@Override
	public PathItem getParent() {
		return parent;
	}
}
