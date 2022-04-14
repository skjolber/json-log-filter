package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class SingleAnyPathMaxSizeJsonFilter extends SingleAnyPathJsonFilter implements RangesJsonFilter {
	
	public SingleAnyPathMaxSizeJsonFilter(int maxPathMatches, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxPathMatches, maxSize, -1, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathMaxSizeJsonFilter(int maxPathMatches, int maxSize, String expression, FilterType type) {
		this(maxPathMatches, maxSize, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(-1, length);

		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, pathChars, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}
		
		ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(-1, length);
		
		try {
			return rangesAnyPathMaxSize(chars, offset, offset + length, offset + maxSize, pathBytes, filterType, maxPathMatches, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static CharArrayRangesBracketFilter rangesAnyPathMaxSize(final char[] chars, int offset, int limit, int maxSizeLimit, final char[] elementPaths, FilterType filterType, int pathMatches, CharArrayRangesBracketFilter filter) {

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
							offset = nextOffset;
							
							continue;
						}
					}
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths)) {
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
							
							mark = offset;
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
								
								mark = offset;
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								if(nextOffset + filter.getAnonymizeMessageLength() >= maxSizeLimit) {
									break loop;
								}
								
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								mark = offset;
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

						if(maxSizeLimit >= limit) {
							// filtering only for path, i.e. keep the rest of the document
							filter.setLevel(0);
							
							return rangesAnyPath(chars, offset, limit, pathMatches, elementPaths, filterType, filter);
						}
					} else {
						offset = nextOffset;
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
	
	public static ByteArrayRangesBracketFilter rangesAnyPathMaxSize(final byte[] chars, int offset, int limit, int maxSizeLimit, final byte[] elementPaths, FilterType filterType, int pathMatches, ByteArrayRangesBracketFilter filter) {

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
							offset = nextOffset;
							
							continue;
						}
					}
					
					// was a field name
					if(matchPath(chars, offset + 1, quoteIndex, elementPaths)) {
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
							offset = ByteArrayRangesFilter.skipSubtree(chars, nextOffset);

							filter.addPrune(nextOffset, offset);
							
							// increment limit since we removed something
							maxSizeLimit += filter.getRemovedLength() - removedLength;
							
							mark = offset;
						} else {
							// special case: anon scalar values
							if(chars[nextOffset] == '"') {
								// quoted value
								if(nextOffset + filter.getAnonymizeMessageLength() >= maxSizeLimit) {
									break loop;
								}

								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								mark = offset;
							} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
								// scalar value
								if(nextOffset + filter.getAnonymizeMessageLength() >= maxSizeLimit) {
									break loop;
								}
								
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);

								filter.addAnon(nextOffset, offset);
								
								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								mark = offset;
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

						if(maxSizeLimit >= limit) {
							// filtering only for path, i.e. keep the rest of the document
							filter.setLevel(0);
							
							return rangesAnyPath(chars, offset, limit, pathMatches, elementPaths, filterType, filter);
						}
					} else {
						offset = nextOffset;
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
	}
	
	protected static ByteArrayRangesBracketFilter rangesMaxSize(final byte[] chars, int offset, int maxSizeLimit, ByteArrayRangesBracketFilter filter) {
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
}
