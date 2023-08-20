package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

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

		int maxReadLimit = offset + length; // i.e. max limit
		
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

						if(level > matches) {
							// so always level < elementPaths.length
							filter.setLevel(bracketLevel);
							filter.setMark(offset);
							
							offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, offset, maxSizeLimit, maxReadLimit, maxStringLength);

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							maxSizeLimit = filter.getMaxSizeLimit();

							continue;
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

						// always skips start object if not on a matching level, so must always constrain here
						matches = level;
						
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
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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
									
									SingleFullPathMaxStringLengthJsonFilter.rangesFullPathMaxStringLength(chars, nextOffset, maxReadLimit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
									
									return filter;
								}
							}
							offset = nextOffset;
							
							continue;
						}

						// reset match for a sibling field name, if any
						matches = level - 1;
						
						// was field name
						if(elementPaths[matches] == STAR_CHARS || matchPath(chars, offset + 1, quoteEndIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset;
							
							continue;
						}

						nextOffset++;
						
						if(matches == elementPaths.length) {
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
										// filter only of max string length
										bracketLevel = 0;

										offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);
										
										break loop;
									}
									
									// filtering only for max size and max string size
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);
									
									bracketLevel = filter.getLevel();
									mark = filter.getMark();
									
									break loop;
								}
							}

							matches--;

							if(maxSizeLimit >= maxReadLimit) {
								// filtering only for full path, i.e. keep the rest of the document
								bracketLevel = 0;
								filter.setLevel(0);
								 rangesFullPathMaxStringLength(chars, offset, maxReadLimit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
								
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
				
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				
				// filter rest of document
				filter.addDelete(markLimit, maxReadLimit);
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

		int maxReadLimit = offset + length; // i.e. max limit
		
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

						if(level > matches) {
							// so always level < elementPaths.length
							filter.setLevel(bracketLevel);
							filter.setMark(offset);
							
							offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, offset, maxSizeLimit, maxReadLimit, maxStringLength);

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							maxSizeLimit = filter.getMaxSizeLimit();

							continue;
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

						// always skips start object if not on a matching level, so must always constrain here
						matches = level;
						
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
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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

								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								// increment limit since we removed something
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
									
									SingleFullPathMaxStringLengthJsonFilter.rangesFullPathMaxStringLength(chars, nextOffset, maxReadLimit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
									
									return filter;
								}
							}
							offset = nextOffset;
							
							continue;
						}
						
						// reset match for a sibling field name, if any
						matches = level - 1;
						
						// was field name
						if(elementPaths[matches] == STAR_BYTES || matchPath(chars, offset + 1, quoteEndIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset;
							
							continue;
						}

						nextOffset++;
						
						if(matches == elementPaths.length) {
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
										// filter only of max string length
										bracketLevel = 0;
										offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);
										break loop;
									}
									
									// filtering only for max size and max string size
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);
									
									bracketLevel = filter.getLevel();
									mark = filter.getMark();
									
									break loop;
								}
							}

							matches--;

							if(maxSizeLimit >= maxReadLimit) {
								// filtering only for full path, i.e. keep the rest of the document
								filter.setLevel(0);
								bracketLevel = 0;
								
								rangesFullPathMaxStringLength(chars, offset, maxReadLimit, pathMatches, maxStringLength, level, elementPaths, matches, filterType, filter);
								
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
				
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				// filter rest of document
				filter.addDelete(markLimit, maxReadLimit);
			}
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

}
