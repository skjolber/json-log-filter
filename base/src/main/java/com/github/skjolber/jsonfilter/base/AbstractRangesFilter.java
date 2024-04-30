package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public abstract class AbstractRangesFilter {

	public static final int MAX_INITIAL_ARRAY_SIZE = 256;
	public static final int DEFAULT_INITIAL_ARRAY_SIZE = 16;

	protected static final int FILTER_PRUNE = 0;
	protected static final int FILTER_ANON = 1;
	protected static final int FILTER_DELETE = 2;
	protected static final int FILTER_MAX_LENGTH = 3;
	
	public static final String FILTER_PRUNE_MESSAGE = "PRUNED";
	public static final String FILTER_PRUNE_MESSAGE_JSON = '"' + FILTER_PRUNE_MESSAGE + '"';
	
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_ANONYMIZE_MESSAGE = '"' + FILTER_ANONYMIZE + '"';
	public static final String FILTER_TRUNCATE_MESSAGE = "... + ";

	protected int[] filter; // start, end, type (if positive) or length (if negative)
	protected int filterIndex = 0;
	
	// calculate the approximate length
	protected final int maxLength;
	protected int removedLength;

	public AbstractRangesFilter(int initialCapacity, int length) {
		if(initialCapacity == -1) {
			initialCapacity = DEFAULT_INITIAL_ARRAY_SIZE;
		} else if(initialCapacity == 0) {
			throw new IllegalArgumentException();
		}
		this.maxLength = length;
		
		this.filter = new int[Math.min(initialCapacity, MAX_INITIAL_ARRAY_SIZE) * 3];
	}
	
	protected void addMaxLength(int start, int end, int length) {
		add(start, end, -length);
	}
	
	public void addAnon(int start, int end) {
		add(start, end, FILTER_ANON);
	}
	
	public void addPrune(int start, int end) {
		add(start, end, FILTER_PRUNE);
	}
	
	public void add(FilterType type, int start, int end) {
		if(type == FilterType.PRUNE) {
			addPrune(start, end);
		} else if(type == FilterType.ANON){
			addAnon(start, end);
		}
	}
	
	
	public void addDelete(int start, int end) {
		add(start, end, FILTER_DELETE);
	}
	
	private void add(int start, int end, int type) {
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

	public void removeLastFilter() {
		filterIndex -= 3;
	}
	
	// for testing
	protected int[] getFilter() {
		return filter;
	}
	
	public int getMaxOutputLength() {
		return maxLength - removedLength;
	}
	
	public static int lengthToDigits(int number) {
		if (number < 100000) {
		    if (number < 100) {
		        if (number < 10) {
		            return 1;
		        } else {
		            return 2;
		        }
		    } else {
		        if (number < 1000) {
		            return 3;
		        } else {
		            if (number < 10000) {
		                return 4;
		            } else {
		                return 5;
		            }
		        }
		    }
		} else {
		    if (number < 10000000) {
		        if (number < 1000000) {
		            return 6;
		        } else {
		            return 7;
		        }
		    } else {
		        if (number < 100000000) {
		            return 8;
		        } else {
		            if (number < 1000000000) {
		                return 9;
		            } else {
		                return 10;
		            }
		        }
		    }
		}
	}
	
	public int getRemovedLength() {
		return removedLength;
	}
}
