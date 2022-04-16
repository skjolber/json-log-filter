package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class MultiPathMaxSizeMaxStringLengthJsonFilter extends MultiPathMaxStringLengthJsonFilter {

	public MultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public MultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;
		
		final int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		final int[] elementMatches = new int[elementFilters.length];

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
						
						break;
					case '}' :
						level--;
						bracketLevel--;

						mark = offset;
						
						if(level < elementFilterStart.length) {
							constrainMatches(elementMatches, level);
						}
						
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
								if(nextOffset - offset > maxStringLength) {
									
									if(offset + maxStringLength > maxSizeLimit) {
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
										// no need to filter for max size
										filter.setLevel(0);
										return rangesMultiPathMaxStringLength(chars, nextOffset, length, maxStringLength, pathMatches, level, elementMatches, elementFilterStart, filter);
									}	
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteIndex);
						}					
								
						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(type != null) {
							if(type == FilterType.PRUNE) {
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
									break loop;
								}
								int removedLength = filter.getRemovedLength();
	
								filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset));
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								mark = offset;
							} else if(type == FilterType.ANON) {
								// special case: anon scalar values
								if(chars[nextOffset] == '"') {
									// quoted value
									if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
										break loop;
									}
									
									int removedLength = filter.getRemovedLength();
	
									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
									
									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
									
									mark = offset;
								} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
									// scalar value
									if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
										break loop;
									}
									
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
	
									int removedLength = filter.getRemovedLength();
	
									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
									
									mark = offset;
								} else {
									int removedLength = filter.getRemovedLength();
									
									// filter as tree
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = filter.anonymizeSubtree(chars, nextOffset, maxSizeLimit);
	
									squareBrackets = filter.getSquareBrackets();
									mark = filter.getMark();
									bracketLevel = filter.getLevel();
	
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
								}
								
							} else if(type == FilterType.DELETE) {
								// TODO
								offset = nextOffset;
							} else {
								offset = nextOffset;
							}
							
							if(offset >= maxSizeLimit) {
								// filtering completed
								break loop;
							}
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									if(maxSizeLimit >= length) {
										// filter only for max string length
										filter.setLevel(0);
										return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
									}
									// filter only for max size and max string length
									filter.setMark(mark);
									filter.setLevel(bracketLevel);

									return MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, length, maxSizeLimit, maxStringLength, filter);
								}
							}
							
							if(maxSizeLimit >= length) {
								// no need to filter for max size
								filter.setLevel(0);
								return rangesMultiPathMaxStringLength(chars, offset, length, maxStringLength, pathMatches, level, elementMatches, elementFilterStart, filter);
							}
						} else {
							offset = nextOffset;
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
				filter.setLevel(bracketLevel);
				filter.setMark(mark);

				filter.alignMark(chars);
				
				// filter rest of document
				filter.addDelete(filter.getMark(), length);
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

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;
		
		final int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		final int[] elementMatches = new int[elementFilters.length];

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
						
						break;
					case '}' :
						level--;
						bracketLevel--;

						mark = offset;
						
						if(level < elementFilterStart.length) {
							constrainMatches(elementMatches, level);
						}
						
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
								if(nextOffset - offset > maxStringLength) {
									
									if(offset + maxStringLength > maxSizeLimit) {
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
										// no need to filter for max size
										filter.setLevel(0);
										return rangesMultiPathMaxStringLength(chars, nextOffset, length, maxStringLength, pathMatches, level, elementMatches, elementFilterStart, filter);
									}
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						FilterType type = null;
						
						// match again any higher filter
						if(level < elementFilterStart.length) {
							type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteIndex);
						}					
								
						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(type != null) {
							if(type == FilterType.PRUNE) {
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
									break loop;
								}
								int removedLength = filter.getRemovedLength();
	
								filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset));
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								mark = offset;
							} else if(type == FilterType.ANON) {
								// special case: anon scalar values
								if(chars[nextOffset] == '"') {
									// quoted value
									if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
										break loop;
									}
									
									int removedLength = filter.getRemovedLength();
	
									offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
									
									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
									
									mark = offset;
								} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
									// scalar value
									if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
										break loop;
									}
									
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
	
									int removedLength = filter.getRemovedLength();
	
									filter.addAnon(nextOffset, offset);
									
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
									
									mark = offset;
								} else {
									int removedLength = filter.getRemovedLength();
									
									// filter as tree
									filter.setLevel(bracketLevel);
									filter.setMark(mark);
									
									offset = filter.anonymizeSubtree(chars, nextOffset, maxSizeLimit);
	
									squareBrackets = filter.getSquareBrackets();
									mark = filter.getMark();
									bracketLevel = filter.getLevel();
	
									// increment limit since we removed something
									maxSizeLimit += filter.getRemovedLength() - removedLength;
								}
								
							} else if(type == FilterType.DELETE) {
								// TODO
								offset = nextOffset;
							} else {
								offset = nextOffset;
							}
							
							if(offset >= maxSizeLimit) {
								// filtering completed
								break loop;
							}
							
							constrainMatchesCheckLevel(elementMatches, level - 1);
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									if(maxSizeLimit >= length) {
										// filter only for max string length
										filter.setLevel(0);
										return MaxStringLengthJsonFilter.ranges(chars, offset, length, maxStringLength, filter);
									}
									// filter only for max size and max string length
									filter.setMark(mark);
									filter.setLevel(bracketLevel);
									
									return MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, length, maxSizeLimit, maxStringLength, filter);
								}
							}
							
							if(maxSizeLimit >= length) {
								// no need to filter for max size
								filter.setLevel(0);
								return rangesMultiPathMaxStringLength(chars, offset, length, maxStringLength, pathMatches, level, elementMatches, elementFilterStart, filter);
							}
						} else {
							offset = nextOffset;
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
				filter.setLevel(bracketLevel);
				filter.setMark(mark);

				filter.alignMark(chars);
				
				// filter rest of document
				filter.addDelete(filter.getMark(), length);
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
	
}
