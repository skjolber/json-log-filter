package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.path.PathItem;

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

		final int maxStringLength = this.maxStringLength + 2; // account for quotes

		final CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(pathMatches, length);

		int maxReadLimit = offset + length; // i.e. max limit
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;
		
		PathItem pathItem = this.pathItem;
		
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
						
						if(anyElementFilters == null && level > pathItem.getLevel()) {							
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							
							int removedLength = filter.getRemovedLength();

							offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, offset + 1, maxSizeLimit, maxReadLimit, maxStringLength);

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;

							if(maxSizeLimit > maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							
							level--;
							
							continue;
						}
						
						break;
					case '}' :
						
						pathItem = pathItem.constrain(level);
						
						level--;
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
								if(quoteIndex - offset >= maxStringLength) {
									
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
									
									if(maxSizeLimit >= maxReadLimit) {
										// no need to filter for max size
										filter.setLevel(0);
										return rangesMultiPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
									}	
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						FilterType type = null;

						pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, quoteIndex);

						if(pathItem.hasType()) {
							// matched
							type = pathItem.getType();
							
							pathItem = pathItem.constrain(level);
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
							int removedLength = filter.getRemovedLength();
							if(type == FilterType.PRUNE) {
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
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
							} else if(type == FilterType.ANON) {
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
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									if(maxSizeLimit >= maxReadLimit) {
										// filter only for max string length
										bracketLevel = 0;
										offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);
										break loop;
									}
									// filter only for max size and max string length
									filter.setMark(mark);
									filter.setLevel(bracketLevel);

									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);
									
									bracketLevel = filter.getLevel();
									mark = filter.getMark();

									break loop;
								}
							}
							
							if(maxSizeLimit >= maxReadLimit) {
								// no need to filter for max size
								filter.setLevel(0);
								return rangesMultiPathMaxStringLength(chars, offset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
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

				int markLimit = filter.markToLimit(chars);
				
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

		int pathMatches = this.maxPathMatches;

		final int maxStringLength = this.maxStringLength + 2; // account for quotes

		final ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(pathMatches, length);

		int maxReadLimit = offset + length;
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;
		
		PathItem pathItem = this.pathItem;
		
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
						
						if(anyElementFilters == null && level > pathItem.getLevel()) {
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							
							int removedLength = filter.getRemovedLength();

							offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, offset + 1, maxSizeLimit, maxReadLimit, maxStringLength);

							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;

							if(maxSizeLimit > maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}

							squareBrackets = filter.getSquareBrackets();
							mark = filter.getMark();
							bracketLevel = filter.getLevel();
							
							level--;
							
							continue;
						}

						break;
					case '}' :
						
						pathItem = pathItem.constrain(level);

						level--;
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
								if(quoteIndex - offset >= maxStringLength) {
									
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
									
									if(maxSizeLimit >= maxReadLimit) {
										// no need to filter for max size
										filter.setLevel(0);
										return rangesMultiPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
									}
								}

								offset = nextOffset;
								
								continue;
							}
						}
						
						// was field name
						FilterType type = null;
						
						// match again any higher filter
						pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, quoteIndex);
						if(pathItem.hasType()) {
							// matched
							type = pathItem.getType();
							
							pathItem = pathItem.constrain(level);
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
							int removedLength = filter.getRemovedLength();
							if(type == FilterType.PRUNE) {
								// is there space within max size?
								if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
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
							} else if(type == FilterType.ANON) {
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
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches <= 0) {
									if(maxSizeLimit >= maxReadLimit) {
										// filter only for max string length
										bracketLevel = 0;
										offset = MaxStringLengthJsonFilter.ranges(chars, offset, maxReadLimit, maxStringLength, filter);
										break loop;
									}
									// filter only for max size and max string length
									filter.setMark(mark);
									filter.setLevel(bracketLevel);
									
									offset = MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, maxSizeLimit, maxStringLength, filter);

									bracketLevel = filter.getLevel();
									mark = filter.getMark();

									break loop;
								}
							}
							
							if(maxSizeLimit >= maxReadLimit) {
								// no need to filter for max size
								filter.setLevel(0);
								return rangesMultiPathMaxStringLength(chars, offset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
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

				int markLimit = filter.markToLimit(chars);
				
				// filter rest of document
				filter.addDelete(markLimit, maxReadLimit);
			}
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}
	
}
