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

	public PathItem constrain(int level) {
		if(this.level <= level) {
			return this;
		}
		return parent.constrain(level);
	}

	public int getLevel() {
		return level;
	}

	public boolean hasType() {
		return false;
	}

	protected static int[][] buildByteDispatch(byte[][] names) {
		int[] count = new int[256];
		for(byte[] name : names) {
			if(name.length > 0) {
				count[name[0] & 0xFF]++;
			}
		}
		int[][] dispatch = new int[256][];
		for(int b = 0; b < 256; b++) {
			if(count[b] > 0) {
				dispatch[b] = new int[count[b]];
			}
		}
		int[] pos = new int[256];
		for(int i = 0; i < names.length; i++) {
			if(names[i].length > 0) {
				int b = names[i][0] & 0xFF;
				dispatch[b][pos[b]++] = i;
			}
		}
		return dispatch;
	}

	protected static int[][] buildCharDispatch(char[][] names) {
		int[] count = new int[256];
		for(char[] name : names) {
			if(name.length > 0) {
				count[name[0] & 0xFF]++;
			}
		}
		int[][] dispatch = new int[256][];
		for(int b = 0; b < 256; b++) {
			if(count[b] > 0) {
				dispatch[b] = new int[count[b]];
			}
		}
		int[] pos = new int[256];
		for(int i = 0; i < names.length; i++) {
			if(names[i].length > 0) {
				int b = names[i][0] & 0xFF;
				dispatch[b][pos[b]++] = i;
			}
		}
		return dispatch;
	}

}
