package com.github.skjolber.jsonfilter.base.path.any;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyPathFilter {
	
	public static AnyPathFilter create(String pathString, FilterType filterType) {
		return new AnyPathFilter(pathString, filterType);
	}
	
	public final String pathString;
	public final char[] pathChars;
	public final byte[] pathBytes;
	
	public final FilterType filterType;
	
	public AnyPathFilter(String pathString, FilterType filterType) {
		this.pathString = pathString;
		this.pathChars = AbstractMultiPathJsonFilter.intern(pathString.toCharArray());
		this.pathBytes = AbstractMultiPathJsonFilter.intern(pathString.getBytes(StandardCharsets.UTF_8));
		this.filterType = filterType;
	}

	public FilterType getFilterType() {
		return filterType;
	}
	
	@Override
	public String toString() {
		return pathString + "[" + Arrays.toString(pathChars) + "]";
	}
}