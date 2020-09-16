package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayAnyPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter implements RangesJsonFilter {

	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final char[] path = this.path;

		length += offset;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches);
		
		length += offset;

		try {
			while(offset < length) {
				if(chars[offset] == '"') {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					nextOffset++;
					
					// is this a field name or a value? A field name must be followed by a colon
					int mark = nextOffset - 1;
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
					
					if(matchPath(chars, offset + 1, mark, path)) {

						nextOffset++;
						if(filterType == FilterType.PRUNE) {
							// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}
							filter.add(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);

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

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}

	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final char[] path = this.path;

		length += offset;

		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);
		
		length += offset;

		try {
			while(offset < length) {
				if(chars[offset] == '"') {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					nextOffset++;
					
					// is this a field name or a value? A field name must be followed by a colon
					int mark = nextOffset - 1;
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
					
					if(matchPath(chars, offset + 1, mark, path)) {

						nextOffset++;
						if(filterType == FilterType.PRUNE) {
							// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}
							filter.add(nextOffset, offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);

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

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}

	}
}
