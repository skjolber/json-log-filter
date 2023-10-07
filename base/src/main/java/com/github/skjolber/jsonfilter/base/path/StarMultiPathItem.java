package com.github.skjolber.jsonfilter.base.path;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class StarMultiPathItem extends PathItem {

	public final String[] fieldNames;
	public final byte[][] fieldNameBytes;
	public final char[][] fieldNameChars;

	public PathItem[] next;
	private PathItem any;

	public StarMultiPathItem(List<String> fieldNames, int index, PathItem previous) {
		this(fieldNames.toArray(new String[fieldNames.size()]), index, previous);
	}
	
	public StarMultiPathItem(String[] fieldNames, int level, PathItem parent) {
		super(level, parent);
		this.fieldNames = fieldNames;
		this.fieldNameBytes = new byte[fieldNames.length][];
		this.fieldNameChars = new char[fieldNames.length][];
		for(int i = 0; i < fieldNames.length; i++) {
			fieldNameBytes[i] = fieldNames[i].getBytes(StandardCharsets.UTF_8);
			fieldNameChars[i] = fieldNames[i].toCharArray();
		}
		this.next = new PathItem[fieldNames.length];
	}

	public void setNext(PathItem next, int i) {
		this.next[i] = next;
	}

	public void setAny(PathItem any) {
		this.any = any;
	}
	
	@Override
	public PathItem matchPath(int level, byte[] source, int start, int end) {
		if(level != this.level) {
			return this;
		}
		byte[][] fieldNameBytes = this.fieldNameBytes;
		for(int i = 0; i < fieldNameBytes.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes[i])) {
				return next[i];
			}
		}
		return any;
	}
	
	@Override
	public PathItem matchPath(int level, char[] source, int start, int end) {
		if(level != this.level) {
			return this;
		}
		char[][] fieldNameChars = this.fieldNameChars;
		for(int i = 0; i < fieldNameChars.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars[i])) {
				return next[i];
			}
		}
		return any;
	}

	@Override
	public PathItem matchPath(int level, String fieldName) {
		if(level != this.level) {
			return this;
		}
		for(int i = 0; i < fieldNames.length; i++) {
			if(fieldName.equals(fieldNames[i])) {
				return next[i];
			}
		}
		return any;
	}

}
