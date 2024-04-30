package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

public class SingleAnyPathMaxSizeMaxStringLengthJsonFilter extends SingleAnyPathMaxStringLengthJsonFilter  {
	
	public SingleAnyPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		int maxStringLength = this.maxStringLength + 2; // account for quotes

		CharArrayRangesSizeFilter filter = getCharArrayRangesBracketFilter(maxPathMatches, length);

		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, maxStringLength, pathChars, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		ByteArrayRangesSizeFilter filter = getByteArrayRangesBracketFilter(maxPathMatches, length);
		
		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, maxStringLength, pathBytes, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static CharArrayRangesSizeFilter rangesAnyPathMaxSize(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, int maxStringLength, final char[] elementPaths, FilterType filterType, int pathMatches, CharArrayRangesSizeFilter filter) {
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
						if(quoteEndIndex - offset >= maxStringLength) {
							// text length too long
							
							if(offset + maxStringLength > maxSizeLimit) {
								// done filtering
								break loop;
							}
							int removedLength = filter.getRemovedLength();

							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteEndIndex, -(offset - 1 + maxStringLength - quoteEndIndex));

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
					
							if(nextOffset <= maxSizeLimit) {
								mark = nextOffset;
							} else {
								filter.removeLastFilter();
								filter.setLevel(bracketLevel);
								filter.addDelete(mark, maxReadLimit);
								return filter;
							}

							if(maxSizeLimit >= maxReadLimit) {
								// filtering only for full path and max string length, i.e. keep the rest of the document
								filter.setLevel(0);
								
								return rangesAnyPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, elementPaths, filterType, filter);
							}
						}
						offset = nextOffset;
						
						continue;
					}
					
					nextOffset++;

					// was a field name
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
									// filter only for max string length
									bracketLevel = 0;
									
									offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);

									break loop;
								}
								
								// filtering only for max size and max string length
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);
								
								maxSizeLimit = filter.getMaxSizeLimit();
								bracketLevel = filter.getLevel();
								mark = filter.getMark();

								break loop;
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filter only for path and max string length
							filter.setLevel(0);
							
							return rangesAnyPathMaxStringLength(chars, offset, maxReadLimit, maxStringLength, pathMatches, elementPaths, filterType, filter);
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

	public static ByteArrayRangesSizeFilter rangesAnyPathMaxSize(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, int maxStringLength, final byte[] elementPaths, FilterType filterType, int pathMatches, ByteArrayRangesSizeFilter filter) {
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
						if(quoteEndIndex - offset >= maxStringLength) {
							// text length too long
							
							if(offset + maxStringLength > maxSizeLimit) {
								// done filtering
								break loop;
							}
							int removedLength = filter.getRemovedLength();

							filter.addMaxLength(chars, offset + maxStringLength - 1, quoteEndIndex, -(offset - 1 + maxStringLength - quoteEndIndex));

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
					
							if(nextOffset <= maxSizeLimit) {
								mark = nextOffset;
							} else {
								filter.removeLastFilter();
								filter.setLevel(bracketLevel);
								filter.addDelete(mark, maxReadLimit);
								return filter;
							}

							if(maxSizeLimit >= maxReadLimit) {
								// filtering only for full path and max string length, i.e. keep the rest of the document
								filter.setLevel(0);
								
								return rangesAnyPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, elementPaths, filterType, filter);
							}
						}
						offset = nextOffset;
						
						continue;
					}
					
					nextOffset++;

					// was a field name
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
									// filter only for max string length
									bracketLevel = 0;
									offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);
									break loop;
								}
								
								// filtering only for max size and max string length
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);
								
								maxSizeLimit = filter.getMaxSizeLimit();
								bracketLevel = filter.getLevel();
								mark = filter.getMark();
								
								break loop;
							}							
						}

						if(maxSizeLimit >= maxReadLimit) {
							// filter only for path and max string length
							filter.setLevel(0);
							
							return rangesAnyPathMaxStringLength(chars, offset, maxReadLimit, maxStringLength, pathMatches, elementPaths, filterType, filter);
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
	

}
