package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter.AnyPathFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

public class AnyPathMaxSizeJsonFilter extends AnyPathJsonFilter {
	
	public AnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public AnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxSize, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		CharArrayRangesSizeFilter filter = getCharArrayRangesBracketFilter(-1, length);

		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, anyElementFilters, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}
		
		ByteArrayRangesSizeFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, anyElementFilters, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	protected static CharArrayRangesSizeFilter rangesAnyPathMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, AnyPathFilter[] anyElementFilters, int pathMatches, CharArrayRangesSizeFilter filter) {

		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '[' :
				case '{' :
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = chars[offset] == '[';
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					offset++;
					mark = offset;

					continue;
				case '}' :
				case ']' :
					bracketLevel--;
					maxSizeLimit++;
					
					offset++;
					mark = offset;

					continue;
				case ',' :
					mark = offset;
					break;
				case '"' :
					int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
					
					int quoteEndIndex = nextOffset;
					
					// is this a field name or a value? A field name must be followed by a colon
					
					// skip over whitespace
					// optimization: scan for highest value
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					while(chars[++nextOffset] <= 0x20);
					
					if(chars[nextOffset] != ':') {
						// was a text value
						offset = nextOffset;
						
						continue;
					}
					// was a field name
					
					// skip colon
					while(chars[++nextOffset] <= 0x20);
					
					FilterType filterType = matchAnyElements(anyElementFilters, chars, offset + 1, quoteEndIndex);
					if(filterType != null) {
						int removedLength = filter.getRemovedLength();
						
						if(filterType == FilterType.PRUNE) {
							// is there space within max size?
							if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
								break loop;
							}
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanQuotedValue(chars, nextOffset);
									
									offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, offset);
								} else {
									offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
								}
							}
							
							filter.addPrune(nextOffset, offset);
							
							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
							
							if(offset <= maxSizeLimit) {
								mark = offset;
							} else {
								filter.removeLastFilter();
								filter.setLevel(bracketLevel);
								filter.addDelete(mark, maxReadLimit);
								return filter;
							}
						} else {
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								// filter as tree
								filter.setLevel(bracketLevel);
								filter.setMark(mark);
								
								offset = filter.anonymizeSubtree(chars, nextOffset, maxSizeLimit);

								squareBrackets = filter.getSquareBrackets();
								mark = filter.getMark();
								bracketLevel = filter.getLevel();

								// increment limit since we removed something
								maxSizeLimit = filter.getMaxSizeLimit();
							} else {
								if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
									break loop;
								}

								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanQuotedValue(chars, nextOffset);
									
									offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, offset);
								} else {
									offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
								}
								
								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								if(offset <= maxSizeLimit) {
									mark = offset;
								} else {
									filter.removeLastFilter();
									filter.setLevel(bracketLevel);
									filter.addDelete(mark, maxReadLimit);
									return filter;
								}									
							}
						}

						if(offset >= maxSizeLimit) {
							// filtering completed
							break loop;
						}

						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								if(maxSizeLimit >= maxReadLimit) {
									// filtering finished, i.e. keep the rest of the document
									filter.setLevel(0);
									return filter;
								}
								
								// filtering only for max size
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								rangesMaxSize(chars, nextOffset, maxReadLimit, maxSizeLimit, filter);
								return filter;
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filtering only for path, i.e. keep the rest of the document
							filter.setLevel(0);
							
							rangesAnyPath(chars, offset, maxReadLimit, pathMatches, anyElementFilters, filter);
							return filter;
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset < maxReadLimit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			if(mark < maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1) {
					// filter rest of document
					filter.addDelete(markLimit, maxReadLimit);
					
					return filter;
				}
			}
			filter.addDelete(mark, maxReadLimit);

		}
		
		return filter;
	}

	protected static CharArrayRangesSizeFilter rangesMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, CharArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = chars[offset] == '[';
					
					bracketLevel++;
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					
					offset++;
					mark = offset;
					
					continue;
				case '}' :
				case ']' :
					maxSizeLimit++;
					bracketLevel--;
					
					offset++;
					mark = offset;
					
					continue;
				case ',' :
					mark = offset;
					break;
				case '"' :					
					offset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
				default : // do nothing
			}
			offset++;			
		}
		
		if(offset < maxReadLimit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			if(mark < maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1) {
					// filter rest of document
					filter.addDelete(markLimit, maxReadLimit);
					
					return filter;
				}
			}
			filter.addDelete(mark, maxReadLimit);
		}
		
		return filter;
	}
	
	protected static ByteArrayRangesSizeFilter rangesAnyPathMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, AnyPathFilter[] anyElementFilters, int pathMatches, ByteArrayRangesSizeFilter filter) {

		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '[' :
				case '{' :
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = chars[offset] == '[';
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					offset++;
					mark = offset;
					
					continue;
				case '}' :
				case ']' :
					bracketLevel--;
					maxSizeLimit++;
					
					offset++;
					mark = offset;
					continue;
				case ',' :
					mark = offset;
					break;
				case '"' :
					int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
					
					int quoteEndIndex = nextOffset;
					
					// is this a field name or a value? A field name must be followed by a colon
					// optimization: scan for highest value
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					while(chars[++nextOffset] <= 0x20);
					
					if(chars[nextOffset] != ':') {
						// was a text value
						offset = nextOffset;
						
						continue;
					}
					// was a field name

					// skip colon
					while(chars[++nextOffset] <= 0x20);

					FilterType filterType = matchAnyElements(anyElementFilters, chars, offset + 1, quoteEndIndex);
					if(filterType != null) {
						int removedLength = filter.getRemovedLength();
						
						if(filterType == FilterType.PRUNE) {
							// is there space within max size?
							if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
								break loop;
							}
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanQuotedValue(chars, nextOffset);
									
									offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, offset);
								} else {
									offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
								}
							}
							
							filter.addPrune(nextOffset, offset);

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
							
							if(offset <= maxSizeLimit) {
								mark = offset;
							} else {
								filter.removeLastFilter();
								filter.setLevel(bracketLevel);
								filter.addDelete(mark, maxReadLimit);
								return filter;
							}
						} else {
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								// filter as tree
								filter.setLevel(bracketLevel);
								filter.setMark(mark);
								
								offset = filter.anonymizeSubtree(chars, nextOffset, maxSizeLimit);

								squareBrackets = filter.getSquareBrackets();
								mark = filter.getMark();
								bracketLevel = filter.getLevel();

								// increment limit since we removed something
								maxSizeLimit = filter.getMaxSizeLimit();
							} else {
								if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
									break loop;
								}

								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanQuotedValue(chars, nextOffset);
									
									offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, offset);
								} else {
									offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
								}
								
								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								if(offset <= maxSizeLimit) {
									mark = offset;
								} else {
									filter.removeLastFilter();
									filter.setLevel(bracketLevel);
									filter.addDelete(mark, maxReadLimit);
									return filter;
								}									
							}
						}
						
						if(offset >= maxSizeLimit) {
							// filtering completed
							break loop;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								if(maxSizeLimit >= maxReadLimit) {
									// filtering finished, i.e. keep the rest of the document
									filter.setLevel(0);
									return filter;
								}
								
								// filtering only for max size
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								return rangesMaxSize(chars, nextOffset, maxReadLimit, maxSizeLimit, filter);
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filtering only for path, i.e. keep the rest of the document
							filter.setLevel(0);
							
							rangesAnyPath(chars, offset, maxReadLimit, pathMatches, anyElementFilters, filter);
							return filter;
						}
					} else {
						offset = nextOffset;
					}
					continue;
					
				default :
			}
			offset++;
		}

		if(offset < maxReadLimit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			if(mark < maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1) {
					// filter rest of document
					filter.addDelete(markLimit, maxReadLimit);
					
					return filter;
				}
			}
			filter.addDelete(mark, maxReadLimit);
		}
		
		return filter;
	}
	
	protected static ByteArrayRangesSizeFilter rangesMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, ByteArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = chars[offset] == '[';
					
					bracketLevel++;
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					offset++;
					mark = offset;
					
					continue;
				case '}' :
				case ']' :
					bracketLevel--;
					maxSizeLimit++;
					
					offset++;
					mark = offset;
					
					continue;
				case ',' :
					mark = offset;
					break;
				case '"' :
					offset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				default : // do nothing
			}
			offset++;			
		}
		
		if(offset < maxReadLimit){
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			if(mark < maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1) {
					// filter rest of document
					filter.addDelete(markLimit, maxReadLimit);
					
					return filter;
				}
			}
			filter.addDelete(mark, maxReadLimit);
		}
		
		return filter;
	}	
}
