package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter.AnyPathFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class AnyPathJsonFilter extends AbstractRangesMultiPathJsonFilter {

	public AnyPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public AnyPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		super(-1, -1, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);
		try {
			AnyPathFilter[] anyElementFilters = this.anyElementFilters;
			if(maxPathMatches != -1) {
				
			} else {
				
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}


	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(maxPathMatches, length);
		try {
			AnyPathFilter[] anyElementFilters = this.anyElementFilters;
			if(maxPathMatches != -1) {
				
			} else {
				
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static <T extends CharArrayRangesFilter> T rangesAnyPath(final char[] chars, int offset, int limit, int pathMatches, AnyPathFilter[] anyElementFilters, T filter) {
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
						offset = nextOffset;
						
						continue;
					}
				}

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				FilterType filterType = matchAnyElements(anyElementFilters, chars, offset + 1, quoteIndex);
				
				if(filterType != null) {
					switch(chars[nextOffset]) {
						case '[':
						case '{':
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
							} else {
								offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
							}
							break;
						case '"': {
							offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							filter.add(filterType, nextOffset, offset);
							break;
						}
						case 't': 
						case 'n': {
							offset = nextOffset + 4;
							filter.add(filterType, nextOffset, offset);
							break;
						}
						case 'f': {
							offset = nextOffset + 5;
							filter.add(filterType, nextOffset, offset);
							break;
						}
						default: {
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							filter.add(filterType, nextOffset, offset);
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

	public static <T extends ByteArrayRangesFilter> T rangesAnyPath(final byte[] chars, int offset, int limit, int pathMatches, AnyPathFilter[] anyElementFilters, T filter) {
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
						offset = nextOffset;
						
						continue;
					}
				}

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				FilterType filterType = matchAnyElements(anyElementFilters, chars, offset + 1, quoteIndex);
				if(filterType != null) {
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
