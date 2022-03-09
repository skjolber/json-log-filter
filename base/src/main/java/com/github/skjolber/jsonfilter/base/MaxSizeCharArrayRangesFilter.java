package com.github.skjolber.jsonfilter.base;

public class MaxSizeCharArrayRangesFilter extends CharArrayRangesFilter {
	
	private int level;
	private boolean[] squareBrackets;
	private int mark;
	
	public MaxSizeCharArrayRangesFilter(int initialCapacity, int maxSize) {
		this(initialCapacity, maxSize, DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public MaxSizeCharArrayRangesFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public void setMax(int mark, int level, boolean[] squareBrackets) {
		this.mark = mark;
		this.level = level;
		this.squareBrackets = squareBrackets;
	}
	
	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer) {
		if(mark != -1) {
			buffer.ensureCapacity(buffer.length() + mark - offset);
			
			super.filter(chars, offset, mark - offset, buffer);
			
			for(int i = level - 1; i >= 0; i--) {
				if(squareBrackets[i]) {
					buffer.append(']');
				} else {
					buffer.append('}');
				}
			}
		} else {
			buffer.ensureCapacity(buffer.length() + getMaxOutputLength());
			
			super.filter(chars, offset, length, buffer);
		}
	}
}
