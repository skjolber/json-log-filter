package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class MultiFullPathJsonFilter extends AbstractRangesMultiPathJsonFilter {

	public MultiFullPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(anyElementFilters != null) {
			throw new IllegalArgumentException("Expected no any-element searches (i.e. '//myField')");
		}
	}

	public MultiFullPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	protected CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		length += offset;

		int level = 0;
		
		PathItem pathItem = this.pathItem;
		
		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level > pathItem.getLevel()) {
							offset = CharArrayRangesFilter.skipObject(chars, offset + 1);
							
							level--;
									
							continue;
						}
						break;
					case '}' :
						pathItem = pathItem.constrain(level);
						
						level--;
						
						break;
					case '"' : { 
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

						// match again any higher filter
						pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, quoteIndex);

						if(pathItem.hasType()) {
							// matched
							FilterType type = pathItem.getType();
							if(chars[nextOffset] == '[') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipArray(chars, nextOffset + 1));
								} else {
									offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
								}
							} else if(chars[nextOffset] == '{') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObject(chars, nextOffset + 1));
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
								if(pathMatches == 0) {
									return filter; // done filtering
								}
							}
							pathItem = pathItem.constrain(level);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(level != 0) {
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}		
	}
	

	@Override
	protected ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		PathItem pathItem = this.pathItem;
		
		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level > pathItem.getLevel()) {
							offset = ByteArrayRangesFilter.skipObject(chars, offset + 1);
							
							level--;
									
							continue;
						}
						break;
					case '}' :
						pathItem = pathItem.constrain(level);
						
						level--;
						
						break;
					case '"' : { 
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

						pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, quoteIndex);

						if(pathItem.hasType()) {
							// matched
							FilterType type = pathItem.getType();
							if(chars[nextOffset] == '[') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipArray(chars, nextOffset + 1));
								} else {
									offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
								}
							} else if(chars[nextOffset] == '{') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObject(chars, nextOffset + 1));
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
								if(pathMatches == 0) {
									return filter; // done filtering
								}
							}
							pathItem = pathItem.constrain(level);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(level != 0) {
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}		
	}
		
}
