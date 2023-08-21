package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SingleFullPathMaxStringLengthJsonFilter extends AbstractRangesSingleCharArrayFullPathJsonFilter {

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

		int level = 0;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		int limit = length + offset;

		try {
			rangesFullPathMaxStringLength(chars, offset, limit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static int rangesFullPathMaxStringLength(final char[] chars, int offset, int limit, int pathMatches, int maxStringLength, int level, final char[][] elementPaths, int matches, FilterType filterType, final CharArrayRangesFilter filter) {
		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' :
					if(level > matches) {
						// so always level < elementPaths.length
						offset = CharArrayRangesFilter.skipObjectMaxStringLength(chars, offset, maxStringLength, filter);
						
						continue;
					}
					level++;
					
					break;
				case '}' :
					level--;
					
					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '"' :
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
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
							if(quoteIndex - offset >= maxStringLength) {
								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
							}

							offset = nextOffset;
							continue;
						}
					}

					// reset match for a sibling field name, if any
					matches = level - 1;

					// was field name
					if(elementPaths[matches] == STAR_CHARS || matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
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
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
							} else {
								offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset);
							} else {
								filter.addAnon(nextOffset, offset);
							}
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// speed up filtering by looking only at max string length
								level = 0;
								
								offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
								
								break loop;
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
	
	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes

		int matches = 0;

		final byte[][] elementPaths = this.pathBytes;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		int limit = length + offset;
		try {
			rangesFullPathMaxStringLength(chars, offset, limit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static int rangesFullPathMaxStringLength(final byte[] chars, int offset, int limit, int pathMatches, int maxStringLength, int level, final byte[][] elementPaths, int matches, FilterType filterType, final ByteArrayRangesFilter filter) {
		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' :
					if(level > matches) {
						// so always level < elementPaths.length
						offset = ByteArrayRangesFilter.skipObjectMaxStringLength(chars, offset, maxStringLength, filter);
						
						continue;
					}
					level++;

					break;
				case '}' :
					level--;
					
					matches = level;
					
					break;
				case '"' :
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
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
							if(quoteIndex - offset >= maxStringLength) {
								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
							}

							offset = nextOffset;
							continue;
						}
					}

					// reset match for a sibling field name, if any
					matches = level - 1;

					if(elementPaths[matches] == STAR_BYTES || matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
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
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
							} else {
								offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset);
							} else {
								filter.addAnon(nextOffset, offset);
							}
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// speed up filtering by looking only at max string length
								level = 0;
								offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
								break loop;
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

}
