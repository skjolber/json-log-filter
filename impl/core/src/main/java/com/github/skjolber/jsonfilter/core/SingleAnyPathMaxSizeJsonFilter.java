package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SingleAnyPathMaxSizeJsonFilter extends SingleAnyPathJsonFilter {
	
	public SingleAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(-1, length);

		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, pathChars, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}
		
		ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, pathBytes, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	protected static CharArrayRangesBracketFilter rangesAnyPathMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, final char[] elementPaths, FilterType filterType, int pathMatches, CharArrayRangesBracketFilter filter) {

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
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
					int quoteEndIndex = nextOffset;
					
					nextOffset++;							
					
					// is this a field name or a value? A field name must be followed by a colon
					
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
					// was a field name
					
					// skip colon
					nextOffset++;
					
					if(elementPaths == STAR_CHARS || matchPath(chars, offset + 1, quoteEndIndex, elementPaths)) {
						int removedLength = filter.getRemovedLength();
						
						while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
							nextOffset++;
						}
						
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
									
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, offset);
								} else {
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
									
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, offset);
								} else {
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							return rangesAnyPath(chars, offset, maxReadLimit, pathMatches, elementPaths, filterType, filter);
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
			
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			
			// filter rest of document
			filter.addDelete(markLimit, maxReadLimit);
		}
		
		return filter;
	}

	protected static CharArrayRangesBracketFilter rangesMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, CharArrayRangesBracketFilter filter) {
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
					do {
						if(chars[offset] == '\\') {
							offset++;
						}
						offset++;
					} while(chars[offset] != '"');

				default : // do nothing
			}
			offset++;			
		}
		
		if(offset < maxReadLimit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			
			// remove rest of document
			filter.addDelete(markLimit, maxReadLimit);
		}
		
		return filter;
	}
	
	protected static ByteArrayRangesBracketFilter rangesAnyPathMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, final byte[] elementPaths, FilterType filterType, int pathMatches, ByteArrayRangesBracketFilter filter) {

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
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
					int quoteEndIndex = nextOffset;
					
					nextOffset++;							
					
					// is this a field name or a value? A field name must be followed by a colon
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
					// was a field name

					// skip colon
					nextOffset++;

					if(elementPaths == STAR_BYTES || matchPath(chars, offset + 1, quoteEndIndex, elementPaths)) {
						int removedLength = filter.getRemovedLength();
						
						while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
							nextOffset++;
						}
						
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
									
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, offset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
									
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, offset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							return rangesAnyPath(chars, offset, maxReadLimit, pathMatches, elementPaths, filterType, filter);
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
			
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			
			// filter rest of document
			filter.addDelete(markLimit, maxReadLimit);
		}
		
		return filter;
	}
	
	protected static ByteArrayRangesBracketFilter rangesMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, ByteArrayRangesBracketFilter filter) {
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
					do {
						if(chars[offset] == '\\') {
							offset++;
						}
						offset++;
					} while(chars[offset] != '"');
					
				default : // do nothing
			}
			offset++;			
		}
		
		if(offset < maxReadLimit){
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			filter.setMaxSizeLimit(maxSizeLimit);
			
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			
			// remove rest of document
			filter.addDelete(markLimit, maxReadLimit);
		}
		
		return filter;
	}	
}
