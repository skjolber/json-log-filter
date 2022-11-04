package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SingleAnyPathJsonFilter extends AbstractRangesSingleCharArrayAnyPathJsonFilter {

	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	protected SingleAnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression,
			FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);
		try {
			return rangesAnyPath(chars, offset, offset + length, maxPathMatches, pathChars, filterType, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(maxPathMatches);
		try {
			return rangesAnyPath(chars, offset, offset + length, maxPathMatches, pathBytes, filterType, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static <T extends CharArrayRangesFilter> T rangesAnyPath(final char[] chars, int offset, int limit, int pathMatches, char[] path, FilterType filterType, T filter) {
		while(offset < limit) {
			if(chars[offset] == '"') {
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

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				if(matchPath(chars, offset + 1, quoteIndex, path)) {
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
							return filter; // done filtering
						}
					}					
				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}

		return filter;
	}

	public static <T extends ByteArrayRangesFilter> T rangesAnyPath(final byte[] chars, int offset, int limit, int pathMatches, byte[] path, FilterType filterType, T filter) {
		while(offset < limit) {
			if(chars[offset] == '"') {
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

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				if(matchPath(chars, offset + 1, quoteIndex, path)) {
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
							return filter; // done filtering
						}
					}					
				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}

		return filter;

	}
}
