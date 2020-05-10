package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayFilter;

public class MaxStringLengthJsonFilter extends AbstractJsonFilter {

	public MaxStringLengthJsonFilter(int maxStringLength) {
		super(maxStringLength);
	}

	public CharArrayFilter ranges(final char[] chars, int offset, int length) {
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		length += offset;

		CharArrayFilter filter = new CharArrayFilter();

		try {
			while(offset < length) {
				if(chars[offset] == '"') {
					int nextOffset = CharArrayFilter.scanBeyondQuotedValue(chars, offset);
					if(nextOffset - offset > maxStringLength) {
						// is this a field name or a value? A field name must be followed by a colon
						
						// special case: no whitespace
						if(chars[nextOffset] == ':') {
							// key
							offset = nextOffset + 1;
						} else {
							// fast-forward over whitespace
							int end = nextOffset;

							// optimization: scan for highest value
							// space: 0x20
							// tab: 0x09
							// carriage return: 0x0D
							// newline: 0x0A

							while(chars[nextOffset] <= 0x20) {
								nextOffset++;
							}
							if(chars[nextOffset] == ':') {
								// was a key
								offset = nextOffset + 1;
							} else {
								// value
								filter.add(offset + maxStringLength - 1, end - 1, offset + maxStringLength - end);
								offset = nextOffset; // +1 since can't be a double quote
							}
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
				}
				offset++;
			}				

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}
	}
}
