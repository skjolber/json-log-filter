package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleFullPathJsonFilter extends AbstractSingleCharArrayFullPathJsonFilter implements RangesJsonFilter {

	protected SingleFullPathJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(type != FilterType.ANON && type != FilterType.PRUNE) {
			throw new IllegalArgumentException();
		}
	}

	public SingleFullPathJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		int matches = 0;

		final char[][] elementPaths = this.pathChars;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		length += offset;

		int level = 0;
		
		length += offset;

		try {
			if(filter(chars, offset, length, level, elementPaths, matches, pathMatches, filter)) {
				return filter;
			}
			return null;
		} catch(Exception e) {
			return null;
		}

	}

	protected boolean filter(final char[] chars, int offset, int length, int level, final char[][] elementPaths, int matches, int pathMatches, final CharArrayRangesFilter filter) {
		while(offset < length) {
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
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
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
								return true; // done filtering
							}							
						}
						
						matches--;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return false;
		}
		
		if(level != 0) {
			return false;
		}

		return true;
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		int matches = 0;

		final byte[][] elementPaths = this.pathBytes;

		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		length += offset;

		try {
			while(offset < length) {
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
						
						if(matches == elementPaths.length) {
							nextOffset++;
							
							if(filterType == FilterType.PRUNE) {
								// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
								while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
									nextOffset++;
								}
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
									return filter; // done filtering
								}							
							}
							
							matches--;
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
		} catch(Exception e) {
			return null;
		}
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
