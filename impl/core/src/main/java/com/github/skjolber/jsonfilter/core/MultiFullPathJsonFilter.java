package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

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
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;
		final int[] elementMatches = new int[elementFilters.length];

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		length += offset;

		int level = 0;
		
		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level >= elementFilterStart.length) {
							// so other always level < elementFilterStart.length
							offset = CharArrayRangesFilter.skipObject(chars, offset);
							
							level--;
									
							continue;
						}
						break;
					case '}' :
						level--;
						
						constrainMatches(elementMatches, level);
						
						break;
					case '"' : { 
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

						// match again any higher filter
						FilterType type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						if(type != null) {
							// matched
							if(type == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset));
							} else {
								if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
									// filter as tree
									offset = CharArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
								} else {
									if(chars[nextOffset] == '"') {
										// quoted value
										offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
									} else {
										offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
									}
									filter.addAnon(nextOffset, offset);
								}
							}
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									return filter; // done filtering
								}
							}
							
							constrainMatches(elementMatches, level - 1);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
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
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level > elementFilterStart.length) {
							offset = ByteArrayRangesFilter.skipObject(chars, offset);
							
							level--;
									
							continue;
						}
						break;
					case '}' :
						level--;
						
						constrainMatches(elementMatches, level);
						
						break;
					case '"' : { 
						if(level >= elementFilterStart.length) {
							// not necessary to check if field or value; missing sub-path
							// so if this is a key, there will never be a full match
							do {
								offset++;
							} while(chars[offset] != '"' || chars[offset - 1] == '\\');
							offset++;							
							
							continue;
						}
						
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

						// match again any higher filter
						FilterType type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						if(type != null) {
							// matched
							if(type == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset));
							} else {
								
								if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
									// filter as tree
									offset = ByteArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
								} else {
									if(chars[nextOffset] == '"') {
										// quoted value
										offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
									} else {
										offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
									}
									filter.addAnon(nextOffset, offset);
								}
							}
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									return filter; // done filtering
								}
							}
							
							constrainMatches(elementMatches, level - 1);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
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
