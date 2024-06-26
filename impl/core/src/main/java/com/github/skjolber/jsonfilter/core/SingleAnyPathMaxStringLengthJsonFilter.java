package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SingleAnyPathMaxStringLengthJsonFilter extends AbstractRangesSingleCharArrayAnyPathJsonFilter {

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
						if(quoteIndex - offset >= maxStringLength) {								
							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
						}

						offset = nextOffset;							
						continue;
					}
				}

				// skip whitespace
				while(chars[++nextOffset] <= 0x20);
				
				if(path == STAR_CHARS || matchPath(chars, offset + 1, quoteIndex, path)) {
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
							// speed up filtering by looking only at max string length
							offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
							break ;
						}
					}
				} else {
					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
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
						if(quoteIndex - offset >= maxStringLength) {								
							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));
						}

						offset = nextOffset;							
						continue;
					}
				}

				// skip whitespace
				while(chars[++nextOffset] <= 0x20);

				if(path == STAR_BYTES || matchPath(chars, offset + 1, quoteIndex, path)) {
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
							// speed up filtering by looking only at max string length
							offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
							break;
						}
					}
				} else {
					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
		}

		return filter;
	}

}
