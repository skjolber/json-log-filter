package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class MultiPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter implements RangesJsonFilter {

	public MultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public MultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;
		
		final int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		final int[] elementMatches = new int[elementFilters.length];

		length += offset;

		int level = 0;
		
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' : 
						level++;
						
						// TODO check level vs matches here, go with max string length only for subtree
						
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
								if(nextOffset - offset > maxStringLength) {								
									filter.add(offset + maxStringLength - 1, mark, offset - 1 + maxStringLength - mark);
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, mark, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, mark);
						}					
								
						nextOffset++;
						if(type == FilterType.PRUNE) {
							// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}
							filter.add(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									// speed up filtering by looking only at max string length
									return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
								}
							}
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
						} else if(type == FilterType.ANON) {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								
								filter.addAnon(nextOffset, offset);
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
							} else {
								// filter as tree
								offset = CharArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
							}
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									// speed up filtering by looking only at max string length
									return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
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
		
		final int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		final int[] elementMatches = new int[elementFilters.length];

		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' : 
						level++;
						
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
								if(nextOffset - offset > maxStringLength) {								
									filter.add(offset + maxStringLength - 1, mark, offset - 1 + maxStringLength - mark);
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, mark, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, mark);
						}					
								
						nextOffset++;
						if(type == FilterType.PRUNE) {
							// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}
							filter.add(nextOffset, offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									// speed up filtering by looking only at max string length
									return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
								}
							}
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
						} else if(type == FilterType.ANON) {
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
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									// speed up filtering by looking only at max string length
									return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
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
