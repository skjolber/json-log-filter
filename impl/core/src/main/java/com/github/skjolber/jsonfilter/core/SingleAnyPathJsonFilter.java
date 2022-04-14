package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SingleAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter implements RangesJsonFilter {

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
				
				if(matchPath(chars, offset + 1, quoteIndex, path)) {

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
						} else if(chars[nextOffset] == 't' 
								|| chars[nextOffset] == 'f' 
								|| (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') 
								|| chars[nextOffset] == '-'
								) {
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
							return filter; // done filtering
						}
					}					
				} else {
					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
		}

		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
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
				
				if(matchPath(chars, offset + 1, quoteIndex, path)) {

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
						} else if(chars[nextOffset] == 't' 
								|| chars[nextOffset] == 'f' 
								|| (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') 
								|| chars[nextOffset] == '-'
								) {
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
				} else {
					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
		}

		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		}

		return filter;

	}
}
