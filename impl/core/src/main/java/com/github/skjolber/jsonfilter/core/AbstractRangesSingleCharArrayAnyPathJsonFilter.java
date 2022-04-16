package com.github.skjolber.jsonfilter.core;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public abstract class AbstractRangesSingleCharArrayAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter {

	public AbstractRangesSingleCharArrayAnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		if(chars.length < offset + length) {
			return false;
		}
		CharArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	protected abstract CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

	protected boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream buffer) {
		if(chars.length < offset + length) {
			return false;
		}
		ByteArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	protected abstract ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length);
}
