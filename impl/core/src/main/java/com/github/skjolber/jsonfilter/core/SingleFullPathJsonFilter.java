package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

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
			offset = rangesFullPath(chars, offset, limit, 0, pathBytes, 0, filterType, maxPathMatches, filter);

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
					
					break;
				case '}' :
					level--;
					
					break;
				case '"' :
					int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
					
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
					// skip whitespace after colon
					while(chars[++nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_CHARS && !matchPath(chars, offset + 1, quoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '[') {
							offset = CharArrayRangesFilter.skipArray(chars, nextOffset + 1);
						} else if(chars[nextOffset] == '{') {
							offset = CharArrayRangesFilter.skipObject(chars, nextOffset + 1);
						} else if(chars[nextOffset] == '"') {
							offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
						continue;
					}
					
					if(level + 1 == elementPaths.length) {
						// matched
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
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
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
								return limit; // done filtering
							}							
						}
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
					
					break;
				case '}' :
					level--;
					
					break;
				case '"' :
					int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

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
					while(chars[++nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_BYTES && !matchPath(chars, offset + 1, quoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '[') {
							offset = ByteArrayRangesFilter.skipArray(chars, nextOffset + 1);
						} else if(chars[nextOffset] == '{') {
							offset = ByteArrayRangesFilter.skipObject(chars, nextOffset + 1);
						} else if(chars[nextOffset] == '"') {
							offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
						continue;
					}
					
					if(level + 1 == elementPaths.length) {
						// matched
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
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
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
								return limit; // done filtering
							}							
						}
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
