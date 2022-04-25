package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class SingleFullPathJsonFilter extends AbstractRangesSingleCharArrayFullPathJsonFilter {

	protected SingleFullPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(type != FilterType.ANON && type != FilterType.PRUNE) {
			throw new IllegalArgumentException();
		}
	}

	public SingleFullPathJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);
		int limit = offset + length;
		try {
			offset = rangesFullPath(chars, offset, limit, 0, pathChars, 0, filterType, maxPathMatches, filter);

			if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}
	
	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(maxPathMatches, length);
		int limit = offset + length;
		try {
			offset = rangesFullPath(chars, offset, offset + length, 0, pathBytes, 0, filterType, maxPathMatches, filter);
			if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	protected static int rangesFullPath(final char[] chars, int offset, int limit, int level, final char[][] elementPaths, int matches, FilterType filterType, int pathMatches, final CharArrayRangesFilter filter) {
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						offset = CharArrayRangesFilter.skipObject(chars, offset);
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
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								// filter as tree
								offset = CharArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {

									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								filter.addAnon(nextOffset, offset);
							}
						}
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								return limit; // done filtering
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

		if(level != 0) {
			throw new IllegalStateException();
		}
		
		return offset;
	}

	protected static int rangesFullPath(final byte[] chars, int offset, int limit, int level, final byte[][] elementPaths, int matches, FilterType filterType, int pathMatches, final ByteArrayRangesFilter filter) {
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						offset = ByteArrayRangesFilter.skipObject(chars, offset);
						
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
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								// filter as tree
								offset = ByteArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								filter.addAnon(nextOffset, offset);
							}
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								return limit; // done filtering
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

		if(level != 0) {
			throw new IllegalStateException();
		}

		return offset;
	}

	protected char[] getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	protected char[] getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	protected char[] getTruncateStringValue() {
		return truncateStringValue;
	}
	
}
