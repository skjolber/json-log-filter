package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.TopLevelString;

public abstract class AbstractRangesJsonFilter extends AbstractJsonFilter {

	public AbstractRangesJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		// a top-level string is rare, and cannot be handled by the regular scanners
		CharArrayRangesFilter copy = TopLevelString.isTopLevelString(chars, offset, length) ? topLevelStringRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		// a top-level string is rare, and cannot be handled by the regular scanners
		CharArrayRangesFilter copy = TopLevelString.isTopLevelString(chars, offset, length) ? topLevelStringRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	protected abstract CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

	public boolean process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer) {
		// a top-level string is rare, and cannot be handled by the regular scanners
		ByteArrayRangesFilter copy = TopLevelString.isTopLevelString(chars, offset, length) ? topLevelStringRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public boolean process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		// a top-level string is rare, and cannot be handled by the regular scanners
		ByteArrayRangesFilter copy = TopLevelString.isTopLevelString(chars, offset, length) ? topLevelStringRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	protected abstract ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length);


	protected CharArrayRangesFilter topLevelStringRanges(final char[] chars, int offset, int length) {
		try {
			return TopLevelString.ranges(chars, offset, length, maxStringLength, false, getCharArrayRangesFilter(-1, length));
		} catch(Exception e) {
			return null;
		}
	}

	protected ByteArrayRangesFilter topLevelStringRanges(final byte[] chars, int offset, int length) {
		try {
			return TopLevelString.ranges(chars, offset, length, maxStringLength, false, getByteArrayRangesFilter(-1, length));
		} catch(Exception e) {
			return null;
		}
	}

	protected CharArrayRangesFilter getCharArrayRangesFilter(int length) {
		return getCharArrayRangesFilter(-1, length);
	}

	protected CharArrayRangesFilter getCharArrayRangesFilter(int capacity, int length) {
		return new CharArrayRangesFilter(capacity, length, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
	}

	protected CharArrayRangesSizeFilter getCharArrayRangesBracketFilter(int capacity, int length) {
		return new CharArrayRangesSizeFilter(capacity, length, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
	}

	protected ByteArrayRangesSizeFilter getByteArrayRangesBracketFilter(int capacity, int length) {
		return new ByteArrayRangesSizeFilter(capacity, length, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
	}

	protected ByteArrayRangesFilter getByteArrayRangesFilter(int length) {
		return getByteArrayRangesFilter(-1, length);
	}
	
	protected ByteArrayRangesFilter getByteArrayRangesFilter(int capacity, int length) {
		return new ByteArrayRangesFilter(capacity, length, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
	}
	
}
