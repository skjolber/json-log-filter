package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleAnyPathMaxStringLengthJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter implements RangesJsonFilter {

	public SingleAnyPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}	
	
	protected SingleAnyPathMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches,
			String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		int maxStringLength = this.maxStringLength + 2; // account for quotes

		final char[] path = this.pathChars;

		length += offset;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		try {
			return rangesAnyPathMaxStringLength(chars, offset, length, maxStringLength, pathMatches, path, filterType, filter);
		} catch(Exception e) {
			return null;
		}

	}

	protected static <T extends CharArrayRangesFilter> T rangesAnyPathMaxStringLength(final char[] chars, int offset, int limit, int maxStringLength,
			int pathMatches, final char[] path, FilterType filterType, final T filter) {
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
						if(nextOffset - offset > maxStringLength) {								
							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
						}

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
							// speed up filtering by looking only at max string length
							return MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
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

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		int maxStringLength = this.maxStringLength + 2; // account for quotes

		final byte[] path = this.pathBytes;

		length += offset;

		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			return rangesAnyPathMaxStringLength(chars, offset, length, maxStringLength, pathMatches, path, filterType, filter);
		} catch(Exception e) {
			return null;
		}

	}

	protected static <T extends ByteArrayRangesFilter> T rangesAnyPathMaxStringLength(final byte[] chars, int offset, int limit, int maxStringLength,
			int pathMatches, final byte[] path, FilterType filterType, final T filter) {
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
						if(nextOffset - offset > maxStringLength) {								
							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
						}

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
							return MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
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
