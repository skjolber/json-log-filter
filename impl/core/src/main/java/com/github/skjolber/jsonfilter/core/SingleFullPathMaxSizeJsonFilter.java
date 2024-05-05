package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

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

		CharArrayRangesSizeFilter filter = getCharArrayRangesBracketFilter(-1, length);

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
		
		ByteArrayRangesSizeFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			return rangesFullPathMaxSize(chars, offset, offset + length, offset + maxSize, 0, pathBytes, 0, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}
	
	public static CharArrayRangesSizeFilter rangesFullPathMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, int level, final char[][] elementPaths, int matches, FilterType filterType, int pathMatches, CharArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					// check corner case
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					offset++;
					
					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;
					level++;

					continue;
				case '}' :
					level--;
					bracketLevel--;
					maxSizeLimit++;
					
					offset++;
					mark = offset;
					
					continue;
				case '[' : {
					// check corner case
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					
					offset++;
					mark = offset;

					continue;
				}
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

					// was field name
					// skip whitespace
					while(chars[++nextOffset] <= 0x20);
					
					if(nextOffset >= maxSizeLimit) {
						break loop;
					}
					
					if(elementPaths[level] != STAR_CHARS && !matchPath(chars, offset + 1, quoteEndIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							maxSizeLimit--;
							if(nextOffset >= maxSizeLimit) {
								break loop;
							}
							
							squareBrackets[bracketLevel] = chars[nextOffset] == '[';
							bracketLevel++;
							
							if(bracketLevel >= squareBrackets.length) {
								squareBrackets = filter.grow(squareBrackets);
							}
							
							// so always level < elementPaths.length
							filter.setLevel(bracketLevel);
							filter.setMark(nextOffset + 1);
							
							offset = filter.skipObjectOrArrayMaxSize(chars, nextOffset + 1, maxSizeLimit);

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							maxSizeLimit = filter.getMaxSizeLimit();
						} else {
							offset = nextOffset;
						}
						continue;
					}
					
					if(level + 1 == elementPaths.length) {
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}

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
									offset = maxSizeLimit;
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
								
								return AnyPathMaxSizeJsonFilter.rangesMaxSize(chars, offset, maxReadLimit, maxSizeLimit, filter);
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filtering only for full path, i.e. keep the rest of the document
							filter.setLevel(0);
							bracketLevel = 0;
							
							rangesFullPath(chars, offset, maxReadLimit, level, elementPaths, matches, filterType, pathMatches, filter);
							
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

	public static ByteArrayRangesSizeFilter rangesFullPathMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, int level, final byte[][] elementPaths, int matches, FilterType filterType, int pathMatches, ByteArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}

					squareBrackets[bracketLevel] = false;
					bracketLevel++;

					offset++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;

					level++;
					continue;
				case '}' :
					maxSizeLimit++;
					level--;
					bracketLevel--;

					offset++;
					mark = offset;

					// always skips start object if not on a matching level, so must always constrain here
					continue;
				case '[' : {
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					
					offset++;
					mark = offset;

					continue;
				}
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
					int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
					
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
					
					// skip whitespace
					while(chars[++nextOffset] <= 0x20);
					
					if(nextOffset >= maxSizeLimit) {
						break loop;
					}
					
					if(elementPaths[level] != STAR_BYTES && !matchPath(chars, offset + 1, quoteEndIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							maxSizeLimit--;
							if(nextOffset >= maxSizeLimit) {
								break loop;
							}
							
							squareBrackets[bracketLevel] = chars[nextOffset] == '[';
							bracketLevel++;
							
							if(bracketLevel >= squareBrackets.length) {
								squareBrackets = filter.grow(squareBrackets);
							}
							
							// so always level < elementPaths.length
							filter.setLevel(bracketLevel);
							filter.setMark(nextOffset + 1);
							
							offset = filter.skipObjectOrArrayMaxSize(chars, nextOffset + 1, maxSizeLimit);

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							maxSizeLimit = filter.getMaxSizeLimit();
						} else {
							offset = nextOffset;
						}
						continue;
					}

					if(level + 1 == elementPaths.length) {
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}

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
								maxSizeLimit = filter.getMaxSizeLimit();
							} else {
								if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
									offset = maxSizeLimit;
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
								
								return AnyPathMaxSizeJsonFilter.rangesMaxSize(chars, offset, maxReadLimit, maxSizeLimit, filter);
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filtering only for full path, i.e. keep the rest of the document
							filter.setLevel(0);
							bracketLevel = 0;
							
							rangesFullPath(chars, offset, maxReadLimit, level, elementPaths, matches, filterType, pathMatches, filter);
							
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
