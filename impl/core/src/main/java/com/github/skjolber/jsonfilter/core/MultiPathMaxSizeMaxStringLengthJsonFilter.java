package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;

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

		AnyPathFilter[] anyElementFilters = this.anyElementFilters;
		
		int pathMatches = this.maxPathMatches;

		final int maxStringLength = this.maxStringLength + 2; // account for quotes

		final CharArrayRangesSizeFilter filter = getCharArrayRangesBracketFilter(pathMatches, length);

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
						
						if(anyElementFilters == null && level > pathItem.getLevel()) {
							
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
						
						pathItem = pathItem.constrain(level);
						
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
									// no need to filter for max size
									filter.setLevel(0);
									
									return rangesMultiPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
								}
							}
							offset = nextOffset;
							
							continue;
						}
						
						// was field name
						FilterType type = null;

						pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, quoteEndIndex);

						if(pathItem.hasType()) {
							// matched
							type = pathItem.getType();
							
							pathItem = pathItem.constrain(level);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteEndIndex);
						}					
								
						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(type != null) {
							int removedLength = filter.getRemovedLength();

							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}

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
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		AnyPathFilter[] anyElementFilters = this.anyElementFilters;

		int pathMatches = this.maxPathMatches;

		final int maxStringLength = this.maxStringLength + 2; // account for quotes

		final ByteArrayRangesSizeFilter filter = getByteArrayRangesBracketFilter(pathMatches, length);

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
						
						if(anyElementFilters == null && level > pathItem.getLevel()) {
							
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
						
						pathItem = pathItem.constrain(level);
						
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
									// no need to filter for max size
									filter.setLevel(0);
									return rangesMultiPathMaxStringLength(chars, nextOffset, maxReadLimit, maxStringLength, pathMatches, level, pathItem, filter);
								}
							}
							offset = nextOffset;
							
							continue;
						}
						
						// was field name
						FilterType type = null;
						
						// match again any higher filter
						pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, quoteEndIndex);
						if(pathItem.hasType()) {
							// matched
							type = pathItem.getType();
							
							pathItem = pathItem.constrain(level);
						}
						
						if(anyElementFilters != null && type == null) {
							type = matchAnyElements(chars, offset + 1, quoteEndIndex);
						}					
								
						nextOffset++;
						
						// skip whitespace
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(type != null) {
							int removedLength = filter.getRemovedLength();

							while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
								nextOffset++;
							}

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
		} catch(Exception e) {
			return null;
		}
	}
	
}
