package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

public abstract class AbstractRangesJsonFilter extends AbstractJsonFilter {

	public AbstractRangesJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		// a document which is not an object or array is rare, and handled separately
		CharArrayRangesFilter copy = isNonStructural(chars, offset, length) ? nonStructuralRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		// a document which is not an object or array is rare, and handled separately
		CharArrayRangesFilter copy = isNonStructural(chars, offset, length) ? nonStructuralRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		buffer.ensureCapacity(buffer.length() + copy.getMaxOutputLength()); 

		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	protected abstract CharArrayRangesFilter ranges(final char[] chars, int offset, int length);

	@Override
	public String process(String jsonString) {
		char[] chars = jsonString.toCharArray();

		// a document which is not an object or array is rare, and handled separately
		CharArrayRangesFilter copy = isNonStructural(chars, 0, chars.length) ? nonStructuralRanges(chars, 0, chars.length) : ranges(chars, 0, chars.length);
		if(copy == null) {
			return null;
		}

		if(copy.isEmpty()) {
			// nothing to filter, as for most documents: the output is the input
			return jsonString;
		}

		StringBuilder output = new StringBuilder(chars.length);
		output.ensureCapacity(copy.getMaxOutputLength());

		copy.filter(chars, 0, chars.length, output);

		return output.toString();
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		// a document which is not an object or array is rare, and handled separately
		ByteArrayRangesFilter copy = isNonStructural(chars, offset, length) ? nonStructuralRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return null;
		}

		// write into an array of the exact output length, so that it can be returned without a copy
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(copy.getOutputLength(length));

		copy.filter(chars, offset, length, output);

		byte[] buffer = output.getBuffer();
		if(output.size() == buffer.length) {
			return buffer;
		}
		return output.toByteArray();
	}

	public boolean process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer) {
		// a document which is not an object or array is rare, and handled separately
		ByteArrayRangesFilter copy = isNonStructural(chars, offset, length) ? nonStructuralRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer);
		
		return true;
	}
	
	public boolean process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		// a document which is not an object or array is rare, and handled separately
		ByteArrayRangesFilter copy = isNonStructural(chars, offset, length) ? nonStructuralRanges(chars, offset, length) : ranges(chars, offset, length);
		if(copy == null) {
			return false;
		}
		
		copy.filter(chars, offset, length, buffer, metrics);
		
		return true;
	}
	
	protected abstract ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length);


	protected CharArrayRangesFilter nonStructuralRanges(final char[] chars, int offset, int length) {
		try {
			return CharArrayRangesFilter.addNonStructural(chars, offset, length, maxStringLength, false, getCharArrayRangesFilter(-1, length));
		} catch(Exception e) {
			return null;
		}
	}

	protected ByteArrayRangesFilter nonStructuralRanges(final byte[] chars, int offset, int length) {
		try {
			return ByteArrayRangesFilter.addNonStructural(chars, offset, length, maxStringLength, false, getByteArrayRangesFilter(-1, length));
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
