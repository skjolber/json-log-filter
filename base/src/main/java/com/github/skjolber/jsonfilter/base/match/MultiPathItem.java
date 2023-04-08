package com.github.skjolber.jsonfilter.base.match;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class MultiPathItem implements PathItem {
	
	public final String[] fieldNames;
	
	public PathItem[] next;
	public final PathItem previous;
	
	public final int index;

	public MultiPathItem(Set<String> keys, int index, PathItem previous) {
		this.fieldNames = new String[keys.size()];
		int i = 0;
		Iterator<String> iterator = keys.iterator();
		while(iterator.hasNext()) {
			fieldNames[i] = iterator.next();
			i++;
		}
		this.next = new PathItem[keys.size()];
		
		this.index = index;
		this.previous = previous;
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

}
