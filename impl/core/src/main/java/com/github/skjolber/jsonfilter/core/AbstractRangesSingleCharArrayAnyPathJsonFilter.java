package com.github.skjolber.jsonfilter.core;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public abstract class AbstractRangesSingleCharArrayAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter {

	public AbstractRangesSingleCharArrayAnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		CharArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	protected abstract CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

	public boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream buffer) {
		ByteArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	
	public boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		ByteArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		CharArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	protected abstract ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length);
	

	protected CharArrayRangesFilter getCharArrayRangesFilter(int length) {
		return getCharArrayRangesFilter(-1, length);
	}

	protected CharArrayRangesFilter getCharArrayRangesFilter(int capacity, int length) {
		return new CharArrayRangesFilter(capacity, length, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
	}

	protected CharArrayRangesBracketFilter getCharArrayRangesBracketFilter(int capacity, int length) {
		return new CharArrayRangesBracketFilter(capacity, length, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
	}

	protected ByteArrayRangesBracketFilter getByteArrayRangesBracketFilter(int capacity, int length) {
		return new ByteArrayRangesBracketFilter(capacity, length, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
	}

	protected ByteArrayRangesFilter getByteArrayRangesFilter(int length) {
		return getByteArrayRangesFilter(-1, length);
	}
	
	protected ByteArrayRangesFilter getByteArrayRangesFilter(int capacity, int length) {
		return new ByteArrayRangesFilter(capacity, length, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
	}
	
}
