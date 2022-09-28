package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilter extends SingleFullPathMaxStringLengthJsonFilter {

	public SingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public SingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		int pathMatches = this.maxPathMatches;

		int matches = 0;

		final char[][] elementPaths = this.pathChars;

		final CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(pathMatches, length);

		length += offset; // i.e. max limit
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;
		
		int mark = 0;
		
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = 0;
		try {
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
							offset++;
							if(offset >= maxSizeLimit) {
								break loop;
							}
							
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							
							int removedLength = filter.getRemovedLength();

							offset = filter.skipObjectMaxSizeMaxStringLength(chars, offset, maxSizeLimit, length, maxStringLength);

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;

							if(maxSizeLimit > length) {
								maxSizeLimit = length;
							}

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();

							if(offset >= maxSizeLimit) {
								// filtering completed
								break loop;
							}
							
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
								if(quoteIndex - offset + 1 > maxStringLength) {
									// text length too long
									
									if(offset + maxStringLength > maxSizeLimit) {
										// done filtering
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

									if(maxSizeLimit >= length) {
										// filtering only for full path and max string length, i.e. keep the rest of the document
										filter.setLevel(0);
										bracketLevel = 0;
										offset = SingleFullPathMaxStringLengthJsonFilter.rangesFullPathMaxStringLength(chars, nextOffset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
										
										break loop;
									}
								}
								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset;
							
							continue;
						}

						nextOffset++;
						
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}
						
						if(matches == elementPaths.length) {
							int removedLength = filter.getRemovedLength();

							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}

							if(filterType == FilterType.PRUNE) {
								// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
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

							if(offset > maxSizeLimit) {
								// filtering completed
								break loop;
							}

							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									if(maxSizeLimit >= length) {
										// filter only of max string length
										bracketLevel = 0;

										offset = MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
										
										break loop;
									}
									
									// filtering only for max size and max string size
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, length, maxSizeLimit, maxStringLength, filter);
									
									bracketLevel = filter.getLevel();
									mark = filter.getMark();
									
									break loop;
								}
							}

							matches--;

							if(maxSizeLimit >= length) {
								// filtering only for full path, i.e. keep the rest of the document
								bracketLevel = 0;
								filter.setLevel(0);
								offset = rangesFullPathMaxStringLength(chars, offset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
								
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

			if(offset < length){
				// max size reached before end of document
				filter.setLevel(bracketLevel);
				filter.setMark(mark);

				int markLimit = filter.markToLimit(chars);
				
				// filter rest of document
				filter.addDelete(markLimit, length);
			}
			
			return filter;
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
		
		int pathMatches = this.maxPathMatches;

		int matches = 0;

		final byte[][] elementPaths = this.pathBytes;

		final ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(pathMatches, length);

		length += offset; // i.e. max limit
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;
		
		int mark = 0;
		
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = 0;
		try {
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
							offset++;
							if(offset >= maxSizeLimit) {
								break loop;
							}
							
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							
							int removedLength = filter.getRemovedLength();

							offset = filter.skipObjectMaxSizeMaxStringLength(chars, offset, maxSizeLimit, length, maxStringLength);

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;

							if(maxSizeLimit > length) {
								maxSizeLimit = length;
							}

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();

							if(offset >= maxSizeLimit) {
								// filtering completed
								break loop;
							}
							
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
								if(quoteIndex - offset + 1 > maxStringLength) {
									// text length too long
									
									if(offset + maxStringLength > maxSizeLimit) {
										// done filtering
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

									if(maxSizeLimit >= length) {
										// filtering only for full path and max string length, i.e. keep the rest of the document
										filter.setLevel(0);
										bracketLevel = 0;
										
										offset = SingleFullPathMaxStringLengthJsonFilter.rangesFullPathMaxStringLength(chars, nextOffset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
										
										break loop;
									}
								}
								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						if(matchPath(chars, offset + 1, quoteIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset;
							
							continue;
						}

						nextOffset++;
						
						if(nextOffset >= maxSizeLimit) {
							break loop;
						}
						
						if(matches == elementPaths.length) {
							int removedLength = filter.getRemovedLength();

							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}

							if(filterType == FilterType.PRUNE) {
								// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
								
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
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

							if(offset > maxSizeLimit) {
								// filtering completed
								break loop;
							}

							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									if(maxSizeLimit >= length) {
										// filter only of max string length
										bracketLevel = 0;
										offset = MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
										break loop;
									}
									
									// filtering only for max size and max string size
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, length, maxSizeLimit, maxStringLength, filter);
									
									bracketLevel = filter.getLevel();
									mark = filter.getMark();
									
									break loop;
								}
							}

							matches--;

							if(maxSizeLimit >= length) {
								// filtering only for full path, i.e. keep the rest of the document
								filter.setLevel(0);
								bracketLevel = 0;
								
								offset = rangesFullPathMaxStringLength(chars, offset, length, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
								
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

			if(offset < length) {
				// max size reached before end of document
				filter.setLevel(bracketLevel);
				filter.setMark(mark);

				int markLimit = filter.markToLimit(chars);
				
				// filter rest of document
				filter.addDelete(markLimit, length);
			}
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

}
