package com.github.skjolber.jsonfilter.base;

public abstract class AbstractRangesFilter {

	protected static final int MAX_INITIAL_ARRAY_SIZE = 256;
	protected static final int DEFAULT_INITIAL_ARRAY_SIZE = 16;

	protected static final int FILTER_PRUNE = 0;
	protected static final int FILTER_ANON = 1;
	protected static final int FILTER_MAX_LENGTH = 2;
	
	protected static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	protected static final String FILTER_PRUNE_MESSAGE_JSON = '"' + FILTER_PRUNE_MESSAGE + '"';
	
	protected static final String FILTER_ANONYMIZE = "*****";
	protected static final String FILTER_ANONYMIZE_MESSAGE = '"' + FILTER_ANONYMIZE + '"';
	protected static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";

	protected int[] filter; // start, end, type (if positive) or length (if negative)
	protected int filterIndex = 0;

	public AbstractRangesFilter(int initialCapacity) {
		if(initialCapacity == -1) {
			initialCapacity = DEFAULT_INITIAL_ARRAY_SIZE;
		} else if(initialCapacity == 0) {
			throw new IllegalArgumentException();
		}
		
		this.filter = new int[Math.min(initialCapacity, MAX_INITIAL_ARRAY_SIZE) * 3];
	}
	
	public void addMaxLength(int start, int end, int length) {
		add(start, end, -length);
	}
	
	public void addAnon(int start, int end) {
		add(start, end, FILTER_ANON);
	}
	
	public void addPrune(int start, int end) {
		add(start, end, FILTER_PRUNE);
	}
	
	public void add(int start, int end, int type) {
		if(filter.length <= filterIndex) {
			
			int[] next = new int[filter.length * 2];
			System.arraycopy(filter, 0, next, 0, filter.length);
			
			filter = next;
		}

		filter[filterIndex++] = start;
		filter[filterIndex++] = end;
		filter[filterIndex++] = type;
	}

	public int getFilterIndex() {
		return filterIndex;
	}

	// for testing
	protected int[] getFilter() {
		return filter;
	}
}
