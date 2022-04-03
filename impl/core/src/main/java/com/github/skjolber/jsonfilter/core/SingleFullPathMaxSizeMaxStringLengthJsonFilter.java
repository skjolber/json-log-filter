package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.BracketStructure;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleFullPathMaxSizeMaxStringLengthJsonFilter extends SingleFullPathJsonFilter implements RangesJsonFilter {

	public SingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public SingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	
	
	
	
	
	
	
	
	
	// må ta imot CharArrayRangesBracketFilter
	
	
	
	
	
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(length <= maxSize) {
			return super.ranges(chars, offset, length);
		}

		int pathMatches = this.maxPathMatches;

		int matches = 0;

		final char[][] elementPaths = this.pathChars;

		final CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(pathMatches, length);

		length += offset; // i.e. max limit
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;
		
		BracketStructure bracketStructure = filter.getBracketStructure(); 
		
		int mark = 0;
		
		boolean[] squareBrackets = bracketStructure.getSquareBrackets();
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
							squareBrackets = bracketStructure.grow(squareBrackets);
						}

						mark = offset;

						if(level > matches + 1) {
							// so always level < elementPaths.length
							
							offset++;
							if(offset >= maxSizeLimit) {
								break loop;
							}
							
							bracketStructure.setLevel(bracketLevel);
							bracketStructure.setMark(mark);
							
							int removedLength = filter.getRemovedLength();

							System.out.println("Skip object at " + bracketLevel + " " + offset + " " + maxSizeLimit);
							System.out.println(new String(chars, 0, offset));
							offset = CharArrayRangesBracketFilter.skipObject(chars, offset, maxSizeLimit, length, maxStringLength, filter, bracketStructure);

							squareBrackets = bracketStructure.getSquareBrackets();
							mark = bracketStructure.getMark();
							bracketLevel = bracketStructure.getLevel();

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;

							if(offset >= maxSizeLimit) {
								System.out.println("Break " + offset + " " + length + " " + maxSizeLimit);
								// filtering completed
								break loop;
							}
							
							System.out.println("NO break");
							
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
							squareBrackets = bracketStructure.grow(squareBrackets);
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
								if(nextOffset - offset > maxStringLength) {
									// text length too long
									
									if(offset + maxStringLength >= maxSizeLimit) {
										// done filtering
										break loop;
									}
									
									int removedLength = filter.getRemovedLength();

									filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex, -(offset - 1 + maxStringLength - quoteIndex));

									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
									
									if(maxSizeLimit >= length) {
										// filtering only for full path and max string length, i.e. keep the rest of the document
										bracketStructure.setLevel(0);
										
										if(ranges(chars, nextOffset, length, level, elementPaths, matches, pathMatches, filter)) {
											return filter;
										}
										return null;
									}
									mark = nextOffset;
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
						
						if(matches == elementPaths.length) {
							nextOffset++;

							if(nextOffset >= maxSizeLimit) {
								break loop;
							}

							int removedLength = filter.getRemovedLength();
							
							if(filterType == FilterType.PRUNE) {
								// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
								while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
									nextOffset++;
								}
								
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() >= maxSizeLimit) {
									break loop;
								}
								offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset);

								filter.addPrune(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
							} else {
								// special case: anon scalar values
								if(chars[nextOffset] == '"') {
									// quoted value
									if(nextOffset + filter.getAnonymizeMessageLength() >= maxSizeLimit) {
										break loop;
									}

									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);

									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
								} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
									// scalar value
									if(nextOffset + filter.getAnonymizeMessageLength() >= maxSizeLimit) {
										break loop;
									}
									
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
								} else {
									// filter as tree
									bracketStructure.setLevel(bracketLevel);
									bracketStructure.setMark(mark);
									
									System.out.println("Anon subtree");
									offset = CharArrayRangesBracketFilter.anonymizeSubtree(chars, nextOffset, maxSizeLimit, filter, bracketStructure);

									squareBrackets = bracketStructure.getSquareBrackets();
									mark = bracketStructure.getMark();
									bracketLevel = bracketStructure.getLevel();

									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;

									if(offset >= maxSizeLimit) {
										// filtering completed
										break loop;
									}
								}
							}
							mark = offset;

							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									if(maxSizeLimit >= length) {
										// filtering finished, i.e. keep the rest of the document
										bracketStructure.setLevel(0);
										return filter;
									}
									
									// filtering only for max size
									return ranges(chars, nextOffset, maxSizeLimit, bracketLevel, mark, filter, bracketStructure);
								}
							}

							matches--;

							if(maxSizeLimit >= length) {
								// filtering only for full path, i.e. keep the rest of the document
								bracketStructure.setLevel(0);
								
								if(ranges(chars, offset, length, level, elementPaths, matches, pathMatches, filter)) {
									return filter;
								}
								return null;
							}
						}
						
						continue;
						
					default :
				}
				offset++;
			}

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			} else if(offset < length) {
				// max size reached before end of document
				bracketStructure.setLevel(bracketLevel);
				bracketStructure.setMark(mark);
				bracketStructure.setSquareBrackets(squareBrackets);

				bracketStructure.alignMark(chars);
				
				System.out.println("Add end " + bracketLevel);
				
				// filter rest of document
				filter.addDelete(bracketStructure.getMark(), length);
			} else {
				// was able to fit the end of the document
				if(bracketLevel != 0) {
					return null;
				}
				
				bracketStructure.setLevel(0);
			}
			
			return filter;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected CharArrayRangesFilter ranges(final char[] chars, int offset, int limit, int bracketLevel, int mark, CharArrayRangesFilter filter, BracketStructure bracketStructure) {
		boolean[] squareBrackets = bracketStructure.getSquareBrackets();
		
		while(offset < limit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
					squareBrackets[bracketLevel] = chars[offset] == '[';
					
					bracketLevel++;
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = bracketStructure.grow(squareBrackets);
					}
					mark = offset;
					
					break;
				case '}' :
				case ']' :
					bracketLevel--;
					// fall through
				case ',' :
					mark = offset;
					break;
				case '"' :					
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					
					continue;
					
				default : // do nothing
			}
			offset++;			
		}
		
		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		} else if(offset < limit) {
			// max size reached before end of document
			bracketStructure.setLevel(bracketLevel);
			bracketStructure.setMark(mark);
			bracketStructure.setSquareBrackets(squareBrackets);

			bracketStructure.alignMark(chars);
			
			// remove rest of document
			filter.addDelete(bracketStructure.getMark(), limit);
		} else {
			// was able to fit the end of the document, so should be level 0
			if(bracketLevel != 0) {
				return null;
			}
			
			bracketStructure.setLevel(0);
		}
		
		return filter;
	}

	
	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		throw new RuntimeException();
	}

	protected char[] getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	protected char[] getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	protected char[] getTruncateStringValue() {
		return truncateStringValue;
	}
	
}
