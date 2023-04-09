package com.github.skjolber.jsonfilter.base.match;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyMultiPathItem implements PathItem {

	public final String[] fieldNames;
	public final byte[][] fieldNameBytes;
	public final char[][] fieldNameChars;

	public PathItem[] next;
	public final PathItem parent;
	
	private PathItem any;
	public final int index;

	public AnyMultiPathItem(List<String> fieldNames, int index, PathItem previous) {
		this(fieldNames.toArray(new String[fieldNames.size()]), index, previous);
	}
	
	public AnyMultiPathItem(String[] fieldNames, int index, PathItem previous) {
		this.fieldNames = fieldNames;
		this.fieldNameBytes = new byte[fieldNames.length][];
		this.fieldNameChars = new char[fieldNames.length][];
		for(int i = 0; i < fieldNames.length; i++) {
			fieldNameBytes[i] = fieldNames[i].getBytes(StandardCharsets.UTF_8);
			fieldNameChars[i] = fieldNames[i].toCharArray();
		}
		this.next = new PathItem[fieldNames.length];
		
		this.index = index;
		this.parent = previous;
	}
	
	@Override
	public String toString() {
		return "AbsolutePathFilter[" + Arrays.toString(fieldNames) + "]";
	}

	@Override
	public PathItem matchPath(String fieldName) {
		for(int i = 0; i < fieldNames.length; i++) {
			if(fieldName.equals(fieldNames[i])) {
				return next[i];
			}
		}
		
		return any;
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
	
	public void setNext(PathItem next, int i) {
		this.next[i] = next;
	}
	
	@Override
	public int getIndex() {
		return index;
	}
	
	public String[] getFieldNames() {
		return fieldNames;
	}
	
	public void setAny(PathItem any) {
		this.any = any;
	}
	
	@Override
	public boolean hasType() {
		return false;
	}
	
	@Override
	public PathItem matchPath(byte[] source, int start, int end) {
		byte[][] fieldNameBytes = this.fieldNameBytes;
		for(int i = 0; i < fieldNameBytes.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes[i])) {
				return next[i];
			}
		}
		return any;
	}
	
	@Override
	public PathItem matchPath(char[] source, int start, int end) {
		char[][] fieldNameChars = this.fieldNameChars;
		for(int i = 0; i < fieldNameChars.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars[i])) {
				return next[i];
			}
		}
		return any;
	}

	@Override
	public PathItem getParent() {
		return parent;
	}

}
