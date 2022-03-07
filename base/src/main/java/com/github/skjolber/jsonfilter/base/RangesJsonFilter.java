package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface RangesJsonFilter extends JsonFilter {

	public default boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		CharArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

	public default boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream buffer) {
		ByteArrayRangesFilter copy = ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		// this might be controversial performance-wise; for heavy filtered documents, it might introduce a
		// bottleneck on memory / cache bandwidth
		// alternative approaches would be to keep track of the diff, and thus know exactly 
		// the proper buffer size
		// TODO no way to ensure capacity 
		
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length);

}
