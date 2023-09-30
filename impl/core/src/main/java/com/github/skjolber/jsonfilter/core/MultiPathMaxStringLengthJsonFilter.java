package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.path.PathItem;


public class MultiPathMaxStringLengthJsonFilter extends AbstractRangesMultiPathJsonFilter {

	public MultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public MultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes,
			String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		final PathItem pathItem = this.pathItem;
		
		try {
			return rangesMultiPathMaxStringLength(chars, offset, offset + length, maxStringLength, pathMatches, 0, pathItem, filter);
		} catch(Exception e) {
			return null;
		}		
	}

	protected CharArrayRangesFilter rangesMultiPathMaxStringLength(final char[] chars, int offset, int limit, final int maxStringLength, int pathMatches, int level, PathItem pathItem, final CharArrayRangesFilter filter) {
		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' : 
					level++;
					
					if(anyElementFilters == null && level > pathItem.getLevel()) {
						offset = CharArrayRangesFilter.skipObjectMaxStringLength(chars, offset + 1, maxStringLength, filter);

						level--;
						
						continue;
					}
					
					break;
				case '}' :
					pathItem = pathItem.constrain(level);

					level--;
					
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
					
					FilterType type = null;
					
					// match again any higher filter
					pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, quoteIndex);
					if(pathItem.hasType()) {
						// matched
						type = pathItem.getType();
						
						pathItem = pathItem.constrain(level);
					}
					
					if(anyElementFilters != null && type == null) {
						type = matchAnyElements(chars, offset + 1, quoteIndex);
					}					
							
					nextOffset++;
					
					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}

					if(type != null) {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(type == FilterType.PRUNE) {
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
							if(type == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset);
							} else {
								filter.addAnon(nextOffset, offset);
							}
						}							
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches <= 0) {
								// speed up filtering by looking only at max string length
								level = 0;
								offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
								break loop;
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
			return null;
		}

		return filter;
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int maxStringLength = this.maxStringLength + 2; // account for quotes

		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		final PathItem pathItem = this.pathItem;

		try {
			return rangesMultiPathMaxStringLength(chars, offset, offset + length, maxStringLength, pathMatches, 0, pathItem, filter);
		} catch(Exception e) {
			return null;
		}		
	}

	protected ByteArrayRangesFilter rangesMultiPathMaxStringLength(final byte[] chars, int offset, int limit, final int maxStringLength, int pathMatches, int level, PathItem pathItem, final ByteArrayRangesFilter filter) {
		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' : 
					level++;
					
					if(anyElementFilters == null && level > pathItem.getLevel()) {
						offset = ByteArrayRangesFilter.skipObjectMaxStringLength(chars, offset + 1, maxStringLength, filter);

						level--;
						
						continue;
					}
					
					break;
				case '}' :
					pathItem = pathItem.constrain(level);

					level--;

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
					
					FilterType type = null;
					
					// match again any higher filter
					pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, quoteIndex);
					if(pathItem.hasType()) {
						// matched
						type = pathItem.getType();
						
						pathItem = pathItem.constrain(level);
					}
					
					if(anyElementFilters != null && type == null) {
						type = matchAnyElements(chars, offset + 1, quoteIndex);
					}					
					
					nextOffset++;
					
					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					if(type != null) {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(type == FilterType.PRUNE) {
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
							if(type == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset);
							} else {
								filter.addAnon(nextOffset, offset);
							}
						}	
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches <= 0) {
								// speed up filtering by looking only at max string length
								level = 0;
								offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
								break loop;
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
			return null;
		}

		return filter;
	}
	
}
