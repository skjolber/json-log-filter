package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface RangesJsonFilter extends JsonFilter {

	public default boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		CharArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

}
