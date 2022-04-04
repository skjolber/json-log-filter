package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleFullPathMaxSizeJsonFilter extends SingleFullPathJsonFilter implements RangesJsonFilter {

	public SingleFullPathMaxSizeJsonFilter(int maxPathMatches, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public SingleFullPathMaxSizeJsonFilter(int maxPathMatches, int maxSize, String expression, FilterType type) {
		this(-1, maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected SingleFullPathMaxSizeJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(length <= maxSize) {
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
		if(length <= maxSize) {
			return super.ranges(chars, offset, length);
		}
		
		ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			//return ranges(chars, offset, offset + length, offset + maxSize, maxStringLength, 0,  filter);
			throw new RuntimeException();
		} catch(Exception e) {
			return null;
		}
	}
	
	public static CharArrayRangesBracketFilter rangesFullPathMaxSize(final char[] chars, int offset, int limit, int maxSizeLimit, int level, final char[][] elementPaths, int matches, FilterType filterType, int pathMatches, CharArrayRangesBracketFilter filter) {

		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
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
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = filter.anonymizeSubtree(chars, nextOffset, maxSizeLimit);

									squareBrackets = filter.getSquareBrackets();
									mark = filter.getMark();
									bracketLevel = filter.getLevel();

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
									if(maxSizeLimit >= limit) {
										// filtering finished, i.e. keep the rest of the document
										filter.setLevel(0);
										return filter;
									}
									
									// filtering only for max size
									filter.setMark(mark);
									filter.setLevel(bracketLevel);
									
									return rangesMaxSize(chars, nextOffset, maxSizeLimit, filter);
								}							
							}

							matches--;

							if(maxSizeLimit >= limit) {
								// filtering only for full path, i.e. keep the rest of the document
								filter.setLevel(0);
								
								if(rangesFullPath(chars, offset, limit, level, elementPaths, matches, filterType, pathMatches, filter)) {
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

			if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			} else if(offset < limit) {
				// max size reached before end of document
				filter.setLevel(bracketLevel);
				filter.setMark(mark);

				filter.alignMark(chars);
				
				// filter rest of document
				filter.addDelete(filter.getMark(), limit);
			} else {
				// was able to fit the end of the document
				if(bracketLevel != 0) {
					return null;
				}
				
				filter.setLevel(0);
			}
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	protected static CharArrayRangesBracketFilter rangesMaxSize(final char[] chars, int offset, int maxSizeLimit, CharArrayRangesBracketFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
					squareBrackets[bracketLevel] = chars[offset] == '[';
					
					bracketLevel++;
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
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
		
		if(offset > maxSizeLimit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		} else if(offset < maxSizeLimit) {
			// max size reached before end of document
			filter.setLevel(bracketLevel);
			filter.setMark(mark);

			filter.alignMark(chars);
			
			// remove rest of document
			filter.addDelete(filter.getMark(), maxSizeLimit);
		} else {
			// was able to fit the end of the document, so should be level 0
			if(bracketLevel != 0) {
				return null;
			}
			
			filter.setLevel(0);
		}
		
		return filter;
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
