package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class MultiPathJsonFilter extends AbstractRangesMultiPathJsonFilter {

	public MultiPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public MultiPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;

		final int[] elementMatches = new int[elementFilters.length];

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);

		length += offset;

		int level = 0;
		
		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' : 
						level++;
						
						if(anyElementFilters == null && level >= elementFilterStart.length) {
							offset = CharArrayRangesFilter.skipObject(chars, offset + 1);

							level--;
							
							continue;
						}
						
						break;
					case '}' :
						level--;
						
						if(level < elementFilterStart.length) {
							constrainMatches(elementMatches, level);
						}
						
						break;
					case '"' :  
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
						
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteIndex);
						}
								
						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						// match again any higher filter
						if(type != null) {
							// matched
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
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
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
		} catch(Exception e) {
			return null;
		}		
	}
	

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;

		final int[] elementMatches = new int[elementFilters.length];

		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(maxPathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' : 
						level++;
						
						if(anyElementFilters == null && level >= elementFilterStart.length) {
							offset = ByteArrayRangesFilter.skipObject(chars, offset + 1);

							level--;
							
							continue;
						}
						
						break;
					case '}' :
						level--;
						
						if(level < elementFilterStart.length) {
							constrainMatches(elementMatches, level);
						}
						
						break;
					case '"' :  
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
						
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteIndex);
						}

						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						// match again any higher filter
						if(type != null) {
							// matched
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
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
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
		} catch(Exception e) {
			return null;
		}		
	}	
}
