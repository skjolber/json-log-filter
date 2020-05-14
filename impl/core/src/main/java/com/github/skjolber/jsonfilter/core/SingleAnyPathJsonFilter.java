package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.CharArrayFilter;
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;

public class SingleAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter {

	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		super(-1, maxPathMatches, expression, type);
	}

	@Override
	public CharArrayFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final char[] path = this.path;

		length += offset;

		CharArrayFilter filter = new CharArrayFilter();

		length += offset;

		try {
			main : 
			while(offset < length) {
				if(chars[offset] == '"') {
					int nextOffset = CharArrayFilter.scanBeyondQuotedValue(chars, offset);

					// is this a field name or a value? A field name must be followed by a colon
					int mark = nextOffset - 1;
					if(chars[nextOffset] != ':') {
						// skip over whitespace
						
						// optimization: scan for highest value
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
							nextOffset++;
						}
						
						if(chars[nextOffset] != ':') {
							// was a text value
							offset = nextOffset;
							
							continue;
						}
					}
					
					if(matchPath(chars, offset + 1, mark, path)) {

						nextOffset++;
						if(filterType == FilterType.PRUNE) {
							// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}
							filter.add(nextOffset, offset = CharArrayFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = CharArrayFilter.scanBeyondUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
							} else {
								// filter as tree
								offset = CharArrayFilter.anonymizeSubtree(chars, nextOffset, filter);
							}
						}
						
						pathMatches--;
						if(pathMatches <= 0) {
							break main; // done filtering
						}
					} else {
						offset = nextOffset;
						
						continue;
					}
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
