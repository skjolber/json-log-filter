package com.github.skjolber.jsonfilter.test.cache;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.truth.JsonFilters;

public class MaxSizeJsonFilterPair {

	@FunctionalInterface
	public interface MaxSizeJsonFilterFunction {
		JsonFilter getMaxSize(int size);
	}

	private JsonFilters infiniteJsonFilter;
	private final MaxSizeJsonFilterFunction delegate;
	private JsonFilter[] filter = new JsonFilter[0];
	
	public MaxSizeJsonFilterPair(JsonFilters infiniteJsonFilter, MaxSizeJsonFilterFunction delegate) {
		this.infiniteJsonFilter = infiniteJsonFilter;
		this.delegate = delegate;
	}
	
	public JsonFilters getInfiniteJsonFilter() {
		return infiniteJsonFilter;
	}
	
	public JsonFilter getMaxSizeJsonFilter(int size) {
		JsonFilter[] filter = this.filter; // defensive copy
		if(size < filter.length) {
			return filter[size];
		}
		JsonFilter[] nextFilter = new JsonFilter[size + 1];
		System.arraycopy(filter, 0, nextFilter, 0, filter.length);
		for(int i = filter.length; i < nextFilter.length; i++) {
			nextFilter[i] = delegate.getMaxSize(i);
		}
		this.filter = nextFilter;
		return nextFilter[size];
	}

}
