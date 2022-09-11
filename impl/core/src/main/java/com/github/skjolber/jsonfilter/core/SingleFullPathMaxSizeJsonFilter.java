package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class SingleFullPathMaxSizeJsonFilter extends SingleFullPathJsonFilter {

	public SingleFullPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public SingleFullPathMaxSizeJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected SingleFullPathMaxSizeJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(-1, length);

		try {
			return rangesFullPathMaxSize(chars, offset, offset + length, offset + maxSize, 0, pathChars, 0, filterType, maxPathMatches, filter);
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
			return rangesFullPathMaxSize(chars, offset, offset + length, offset + maxSize, 0, pathBytes, 0, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}
	
	public static CharArrayRangesBracketFilter rangesFullPathMaxSize(final char[] chars, int offset, int limit, int maxSizeLimit, int level, final char[][] elementPaths, int matches, FilterType filterType, int pathMatches, CharArrayRangesBracketFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					level++;

					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;

					if(level > matches + 1) {
						// so always level < elementPaths.length
						filter.setLevel(bracketLevel);
						filter.setMark(mark);
						
						offset = filter.skipObjectMaxSize(chars, offset + 1, maxSizeLimit);

						squareBrackets = filter.getSquareBrackets();
						mark = filter.getMark();
						bracketLevel = filter.getLevel();

						// counted offset bracket twice
						level--;
						
						continue;
					}
					break;
				case '}' :
					level--;
					bracketLevel--;

					mark = offset;

					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '[' : {
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					mark = offset;

					break;
				}
				case ']' :
					bracketLevel--;
					
					mark = offset;

					break;
				case ',' :
					mark = offset;
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
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset;
						continue;
					}

					nextOffset++;

					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(matches == elementPaths.length) {
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}

						int removedLength = filter.getRemovedLength();
						
						if(filterType == FilterType.PRUNE) {
							// is there space within max size?
							if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
								offset = maxSizeLimit;
								break loop;
							}
							offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset);

							filter.addPrune(nextOffset, offset);
							
							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
							
							if(offset < maxSizeLimit) {
								mark = offset;
							} else {
								filter.removeLastFilter();
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
								maxSizeLimit += filter.getRemovedLength() - removedLength;

								
							} else {
								if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
									offset = maxSizeLimit;
									break loop;
								}

								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								
								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								if(offset < maxSizeLimit) {
									mark = offset;
								} else {
									filter.removeLastFilter();
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
								if(maxSizeLimit >= limit) {
									// filtering finished, i.e. keep the rest of the document
									filter.setLevel(0);
									return filter;
								}
								
								// filtering only for max size
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								return SingleAnyPathMaxSizeJsonFilter.rangesMaxSize(chars, offset, limit, maxSizeLimit, filter);
							}							
						}

						matches--;

						if(maxSizeLimit >= limit) {
							// filtering only for full path, i.e. keep the rest of the document
							filter.setLevel(0);
							bracketLevel = 0;
							
							offset = rangesFullPath(chars, offset, limit, level, elementPaths, matches, filterType, pathMatches, filter);
							
							break loop;
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		} else if(offset < limit){
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);

			filter.alignMark(chars);
			
			// filter rest of document
			filter.addDelete(filter.getMark(), limit);
		}
		
		return filter;
	}

	public static ByteArrayRangesBracketFilter rangesFullPathMaxSize(final byte[] chars, int offset, int limit, int maxSizeLimit, int level, final byte[][] elementPaths, int matches, FilterType filterType, int pathMatches, ByteArrayRangesBracketFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					level++;

					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;

					if(level > matches + 1) {
						// so always level < elementPaths.length
						filter.setLevel(bracketLevel);
						filter.setMark(mark);
						
						offset = filter.skipObjectMaxSize(chars, offset + 1, maxSizeLimit);

						squareBrackets = filter.getSquareBrackets();
						mark = filter.getMark();
						bracketLevel = filter.getLevel();

						// counted offset bracket twice
						level--;
						
						continue;
					}
					break;
				case '}' :
					level--;
					bracketLevel--;

					mark = offset;

					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '[' : {
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					mark = offset;

					break;
				}
				case ']' :
					bracketLevel--;
					
					mark = offset;

					break;
				case ',' :
					mark = offset;
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
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset;
						continue;
					}

					nextOffset++;

					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(matches == elementPaths.length) {
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}

						int removedLength = filter.getRemovedLength();
						
						if(filterType == FilterType.PRUNE) {
							
							// is there space within max size?
							if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
								offset = maxSizeLimit;
								break loop;
							}
							offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset);

							filter.addPrune(nextOffset, offset);
							
							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
							
							if(offset < maxSizeLimit) {
								mark = offset;
							} else {
								filter.removeLastFilter();
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
								maxSizeLimit += filter.getRemovedLength() - removedLength;
							} else {
								if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
									offset = maxSizeLimit;
									break loop;
								}

								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								
								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								if(offset < maxSizeLimit) {
									mark = offset;
								} else {
									filter.removeLastFilter();
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
								if(maxSizeLimit >= limit) {
									// filtering finished, i.e. keep the rest of the document
									filter.setLevel(0);
									return filter;
								}
								
								// filtering only for max size
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								return SingleAnyPathMaxSizeJsonFilter.rangesMaxSize(chars, offset, limit, maxSizeLimit, filter);
							}							
						}

						matches--;

						if(maxSizeLimit >= limit) {
							// filtering only for full path, i.e. keep the rest of the document
							filter.setLevel(0);
							bracketLevel = 0;
							
							offset = rangesFullPath(chars, offset, limit, level, elementPaths, matches, filterType, pathMatches, filter);
							
							break loop;
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		} else if(offset < limit){
			// max size reached before end of document

			filter.setLevel(bracketLevel);
			filter.setMark(mark);

			filter.alignMark(chars);
			
			// filter rest of document
			filter.addDelete(filter.getMark(), limit);
		}
		
		return filter;
	}
	
}
