package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.CharArrayFilter;
import com.github.skjolber.jsonfilter.base.SingleCharArrayFullPathJsonFilter;

public class SingleFullPathMaxStringLengthJsonFilter extends SingleCharArrayFullPathJsonFilter {

	public SingleFullPathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type) {
		super(maxStringLength, expression, type);
	}

	@Override
	public CharArrayFilter ranges(final char[] chars, int offset, int length) {
		int maxStringLength = this.maxStringLength + 2; // account for quotes

		int matches = 0;

		final char[][] elementPaths = this.paths;

		length += offset;

		int level = 0;
		
		CharArrayFilter filter = new CharArrayFilter();

		length += offset;

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' : {
						level++;
						
						break;
					}
					case '}' : {
						level--;
						
						if(matches >= level) {
							matches = level;
						}
						
						break;
					}
					case '"' : { 
						int nextOffset = CharArrayFilter.scanBeyondQuotedValue(chars, offset);

						// is this a field name or a value? A field name must be followed by a colon
						int mark = nextOffset - 1;
						if(chars[nextOffset] != ':') {
							// skip over whitespace
							// TODO skip until common character?
							
							// while(chars[nextOffset] == ' ' || chars[nextOffset] == '\n' || chars[nextOffset] == '\t' || chars[nextOffset] == '\r') {
							// while(chars[nextOffset] != ',' && chars[nextOffset] != ']' && chars[nextOffset] != '}') {

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
								if(nextOffset - offset > maxStringLength) {								
									filter.add(offset + maxStringLength - 1, mark, offset - 1 + maxStringLength - mark);
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						if(matches + 1 == level) {
						
							if(matchPath(chars, offset + 1, mark, elementPaths[matches])) {
								matches++;
							} else {
								offset = nextOffset;
								
								continue;
							}
							
							if(matches == elementPaths.length) {
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
								matches--;
							}
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default :
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
