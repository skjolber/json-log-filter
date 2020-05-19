package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractRangesJsonFilter extends AbstractJsonFilter implements JsonFilter {

	public AbstractRangesJsonFilter(int maxStringLength) {
		super(maxStringLength) ;
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		CharArrayFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public abstract CharArrayFilter ranges(final char[] chars, int offset, int length);

}
