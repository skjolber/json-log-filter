package com.github.skjolber.jsonfilter.base.path;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class MultiPathItem extends PathItem {
	
	private final String[] fieldNames;
	private final byte[][] fieldNameBytes;
	private final char[][] fieldNameChars;

	private PathItem[] next;

	/** {@code byteDispatch[b]} lists the indices {@code i} where {@code fieldNameBytes[i][0] & 0xFF == b}. */
	private final int[][] byteDispatch;
	/** {@code charDispatch[c]} lists the indices {@code i} where {@code fieldNameChars[i][0] & 0xFF == c}. */
	private final int[][] charDispatch;

	public MultiPathItem(List<String> fieldNames, int index, PathItem previous) {
		this(fieldNames.toArray(new String[fieldNames.size()]), index, previous);
	}
	
	public MultiPathItem(String[] fieldNames, int level, PathItem parent) {
		super(level, parent);
		
		// note: always expect two or more field names
		
		this.fieldNames = fieldNames;
		this.fieldNameBytes = new byte[fieldNames.length][];
		this.fieldNameChars = new char[fieldNames.length][];
		for(int i = 0; i < fieldNames.length; i++) {
			fieldNameBytes[i] = fieldNames[i].getBytes(StandardCharsets.UTF_8);
			fieldNameChars[i] = fieldNames[i].toCharArray();
		}
		this.next = new PathItem[fieldNames.length];
		this.byteDispatch = buildByteDispatch(fieldNameBytes);
		this.charDispatch = buildCharDispatch(fieldNameChars);
	}

	public void setNext(PathItem next, int i) {
		this.next[i] = next;
	}
	
	@Override
	public PathItem matchPath(int level, byte[] source, int start, int end) {
		if(level != this.level) {
			return this;
		}
		if(start < end && source[start] != '\\') {
			// fast path: dispatch by first byte, then check only the sub-bucket
			int[] candidates = byteDispatch[source[start] & 0xFF];
			if(candidates != null) {
				byte[][] fieldNameBytes = this.fieldNameBytes;
				for(int idx : candidates) {
					if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes[idx])) {
						return next[idx];
					}
				}
			}
			return this;
		}
		// slow path: encoded key (starts with '\') or empty key — linear scan
		byte[][] fieldNameBytes = this.fieldNameBytes;
		for(int i = 0; i < fieldNameBytes.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes[i])) {
				return next[i];
			}
		}
		return this;
	}
	
	@Override
	public PathItem matchPath(int level, char[] source, int start, int end) {
		if(level != this.level) {
			return this;
		}
		if(start < end && source[start] != '\\') {
			int[] candidates = charDispatch[source[start] & 0xFF];
			if(candidates != null) {
				char[][] fieldNameChars = this.fieldNameChars;
				for(int idx : candidates) {
					if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars[idx])) {
						return next[idx];
					}
				}
			}
			return this;
		}
		char[][] fieldNameChars = this.fieldNameChars;
		for(int i = 0; i < fieldNameChars.length; i++) {
			if(AbstractPathJsonFilter.matchPath(source, start, end, fieldNameChars[i])) {
				return next[i];
			}
		}
		return this;
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
		
		return this;
	}

}
