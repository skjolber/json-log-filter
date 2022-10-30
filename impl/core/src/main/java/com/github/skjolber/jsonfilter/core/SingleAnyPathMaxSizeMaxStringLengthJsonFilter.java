package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

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

		CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(-1, length);

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
		
		ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, maxStringLength, pathBytes, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static CharArrayRangesBracketFilter rangesAnyPathMaxSize(final char[] chars, int offset, int limit, int maxSizeLimit, int maxStringLength, final char[] elementPaths, FilterType filterType, int pathMatches, CharArrayRangesBracketFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;
					break;
				case '}' :
					bracketLevel--;

					mark = offset;

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
							if(quoteIndex - offset + 1 > maxStringLength) {
								// text length too long
								
								if(offset + maxStringLength > maxSizeLimit) {
									// done filtering
									offset = maxSizeLimit;
									
									break loop;
								}
								
								int removedLength = filter.getRemovedLength();

								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));

								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
						
								if(nextOffset >= maxSizeLimit) {
									filter.removeLastFilter();
									
									offset = nextOffset;
									
									break loop;
								}

								if(maxSizeLimit >= limit) {
									// filtering only for full path and max string length, i.e. keep the rest of the document
									filter.setLevel(0);
									
									return rangesAnyPathMaxStringLength(chars, nextOffset, limit, maxStringLength, pathMatches, elementPaths, filterType, filter);
								}
							}
							offset = nextOffset;
							
							continue;
						}
					}

					nextOffset++;

					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths)) {
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
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
							}
							
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
									// filter only for max string length
									bracketLevel = 0;
									
									offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);

									break loop;
								}
								
								// filtering only for max size and max string length
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, limit, maxSizeLimit, maxStringLength, filter);
								
								bracketLevel = filter.getLevel();
								mark = filter.getMark();

								break loop;
							}							
						}

						if(maxSizeLimit >= limit) {
							// filter only for path and max string length
							filter.setLevel(0);
							
							return rangesAnyPathMaxStringLength(chars, offset, limit, maxStringLength, pathMatches, elementPaths, filterType, filter);
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset < limit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);

			int markLimit = filter.markToLimit(chars);
			
			// filter rest of document
			filter.addDelete(markLimit, limit);
		}
		
		return filter;
	}

	public static ByteArrayRangesBracketFilter rangesAnyPathMaxSize(final byte[] chars, int offset, int limit, int maxSizeLimit, int maxStringLength, final byte[] elementPaths, FilterType filterType, int pathMatches, ByteArrayRangesBracketFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					mark = offset;
					break;
				case '}' :
					bracketLevel--;

					mark = offset;
					
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
							if(quoteIndex - offset + 1 > maxStringLength) {
								// text length too long
								
								if(offset + maxStringLength > maxSizeLimit) {
									// done filtering
									offset = maxSizeLimit;
									
									break loop;
								}
								
								int removedLength = filter.getRemovedLength();

								filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));

								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;

								if(nextOffset >= maxSizeLimit) {
									filter.removeLastFilter();
									
									offset = nextOffset;
									
									break loop;
								}

								if(maxSizeLimit >= limit) {
									// filtering only for full path and max string length, i.e. keep the rest of the document
									filter.setLevel(0);
									
									return rangesAnyPathMaxStringLength(chars, nextOffset, limit, maxStringLength, pathMatches, elementPaths, filterType, filter);
								}
							}
							offset = nextOffset;
							
							continue;
						}
					}

					nextOffset++;

					// skip whitespace
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths)) {
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
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
							}
							
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
									// filter only for max string length
									bracketLevel = 0;
									offset = MaxStringLengthJsonFilter.ranges(chars, offset, limit, maxStringLength, filter);
									break loop;
								}
								
								// filtering only for max size and max string length
								filter.setMark(mark);
								filter.setLevel(bracketLevel);
								
								offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, limit, maxSizeLimit, maxStringLength, filter);
								
								bracketLevel = filter.getLevel();
								mark = filter.getMark();
								
								break loop;
							}							
						}

						if(maxSizeLimit >= limit) {
							// filter only for path and max string length
							filter.setLevel(0);
							
							return rangesAnyPathMaxStringLength(chars, offset, limit, maxStringLength, pathMatches, elementPaths, filterType, filter);
						}
					} else {
						offset = nextOffset;
					}
					
					continue;
					
				default :
			}
			offset++;
		}

		if(offset < limit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);
			
			int markLimit = filter.markToLimit(chars);

			// filter rest of document
			filter.addDelete(markLimit, limit);
		}
		
		return filter;
	}
	

}
