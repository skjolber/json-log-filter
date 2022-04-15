package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleFullPathMaxStringLengthJsonFilter extends AbstractSingleCharArrayFullPathJsonFilter implements RangesJsonFilter {

	public SingleFullPathMaxStringLengthJsonFilter(int maxStringLength,  int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathMaxStringLengthJsonFilter(int maxStringLength,  int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}	
	
	protected SingleFullPathMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes

		int matches = 0;

		final char[][] elementPaths = this.pathChars;
		
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		length += offset;

		int level = 0;

		try {
			return rangesFullPathMaxStringLength(chars, offset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static CharArrayRangesFilter rangesFullPathMaxStringLength(final char[] chars, int offset, int length,
			int pathMatches, int maxStringLength, int level, final char[][] elementPaths,
			int matches, FilterType filterType, final CharArrayRangesFilter filter) {
		while(offset < length) {
			switch(chars[offset]) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						offset = CharArrayRangesFilter.skipObjectMaxStringLength(chars, offset, maxStringLength, filter);
						
						level--;
						
						continue;
					}
					
					break;
				case '}' :
					level--;
					
					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '"' :
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					int quoteIndex = nextOffset;
					
					nextOffset++;
					
					// is this a field name or a value? A field name must be followed by a colon
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
							if(quoteIndex - offset + 1 > maxStringLength) {
								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
							}

							offset = nextOffset;
							
							continue;
						}
					}
					
					// was field name
				
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset;
						
						continue;
					}

					nextOffset++;
					
					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(matches == elementPaths.length) {
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset));
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
							} else {
								// filter as tree
								offset = CharArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
							}
						}
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// speed up filtering by looking only at max string length
								return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
							}
						}
						
						matches--;
					} else {
						offset = nextOffset;
					}
					
					continue;
				
				default :
			}
			offset++;
		}

		if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		}

		if(level != 0) {
			return null;
		}

		return filter;
	}
	
	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes

		int matches = 0;

		final byte[][] elementPaths = this.pathBytes;

		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			return rangesFullPathMaxStringLength(chars, offset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static ByteArrayRangesFilter rangesFullPathMaxStringLength(final byte[] chars, int offset, int length,
		int pathMatches, int maxStringLength, int level, final byte[][] elementPaths,
		int matches, FilterType filterType, final ByteArrayRangesFilter filter) {
		while(offset < length) {
			switch(chars[offset]) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						offset = ByteArrayRangesFilter.skipObjectMaxStringLength(chars, offset, maxStringLength, filter);
						
						level--;
						
						continue;
					}

					break;
				case '}' :
					level--;
					
					if(matches >= level) {
						matches = level;
					}
					
					break;
				case '"' :
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					int quoteIndex = nextOffset;
					
					nextOffset++;
					
					// is this a field name or a value? A field name must be followed by a colon
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
							if(nextOffset - offset > maxStringLength) {								
								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
							}

							offset = nextOffset;
							
							continue;
						}
					}
					
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset;
						
						continue;
					}

					nextOffset++;
					
					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(matches == elementPaths.length) {
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset));
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
							} else {
								// filter as tree
								offset = ByteArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
							}
						}
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// speed up filtering by looking only at max string length
								return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
							}
						}
						
						matches--;
					} else {
						offset = nextOffset;
					}
					
					continue;
				
				default :
			}
			offset++;
		}

		if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		}

		if(level != 0) {
			return null;
		}

		return filter;
	}

}
