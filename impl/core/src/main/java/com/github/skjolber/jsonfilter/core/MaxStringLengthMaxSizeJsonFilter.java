/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class MaxStringLengthMaxSizeJsonFilter extends MaxStringLengthJsonFilter {

	public MaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize) {
		this(maxStringLength, maxSize, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(!mustConstrainMaxSize(length)) {
			return super.ranges(chars, offset, length);
		}

		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		CharArrayRangesSizeFilter filter = getCharArrayRangesBracketFilter(-1, length);

		int maxReadLimit = offset + length;

		try {
			offset = rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, offset + maxSize, maxStringLength, filter);
			
			if(offset < maxReadLimit) {
				// max size reached before end of document
				if(filter.getMark() < filter.getMaxSizeLimit()) {
					int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, filter.getMaxSizeLimit(), filter.getMark());
					if(markLimit != -1) {
						// filter rest of document
						filter.addDelete(markLimit, maxReadLimit);
						
						return filter;
					}
				}
				filter.addDelete(filter.getMark(), maxReadLimit);
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
		
		ByteArrayRangesSizeFilter filter = getByteArrayRangesBracketFilter(-1, length);

		int maxReadLimit = offset + length;
		
		try {
			offset = rangesMaxSizeMaxStringLength(chars, offset, maxReadLimit, offset + maxSize, maxStringLength, filter);
			
			if(offset < maxReadLimit) {
				// max size reached before end of document
				if(filter.getMark() < filter.getMaxSizeLimit()) {
					int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, filter.getMaxSizeLimit(), filter.getMark());
					if(markLimit != -1) {
						// filter rest of document
						filter.addDelete(markLimit, maxReadLimit);
						
						return filter;
					}
				}
				filter.addDelete(filter.getMark(), maxReadLimit);
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static int rangesMaxSizeMaxStringLength(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, int maxStringLength, CharArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '[' :
				case '{' :
					// check corner case
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
					
					if(nextOffset - offset < maxStringLength) {
						offset = nextOffset + 1;
						continue;
					}

					nextOffset++;					
					
					// is this a field name or a value? A field name must be followed by a colon
					
					// special case: no whitespace
					if(chars[nextOffset] == ':') {
						// key
						offset = nextOffset + 1;
						continue;
					}
					// most likely there is now no whitespace, but a comma, end array or end object
					
					// legal whitespaces are:
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					int quoteIndex = nextOffset;
					if(chars[nextOffset] <= 0x20) {
						// fast-forward over whitespace
						// optimization: scan for highest value
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// was a field name
							offset = nextOffset + 1;
							continue;
						}
					}
					
					if(offset + maxStringLength > maxSizeLimit) {
						offset = nextOffset;
						
						break loop;
					}
					
					int removedLength = filter.getRemovedLength();
					if(filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex - 1, -(offset + maxStringLength - quoteIndex))) {
						// increment limit since we removed something
						maxSizeLimit += filter.getRemovedLength() - removedLength;

						if(nextOffset > maxSizeLimit) {
							// corner case, even with removal we are over the limit
							maxSizeLimit -= filter.getRemovedLength() - removedLength;

							filter.removeLastFilter();
							
							offset = nextOffset;
							
							break loop;
						}
						
						if(maxSizeLimit >= maxReadLimit) {
							// filter only for max string length
							filter.setLevel(0);
							
							return ranges(chars, nextOffset, maxReadLimit, maxStringLength, filter);
						}
					}
					mark = nextOffset;
					offset = nextOffset;
					
					continue;
				default :
			}
			offset++;
		}

		filter.setLevel(bracketLevel);
		filter.setMark(mark);
		filter.setMaxSizeLimit(maxSizeLimit);

		return offset;
	}
	
	public static int rangesMaxSizeMaxStringLength(final byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, int maxStringLength, ByteArrayRangesSizeFilter filter) {
		boolean[] squareBrackets = filter.getSquareBrackets();
		int bracketLevel = filter.getLevel();
		int mark = filter.getMark();
		
		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '[' : 
				case '{' :
					// check corner case
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
				case ']' :
				case '}' :	
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

					if(nextOffset - offset < maxStringLength) {
						offset = nextOffset + 1;
						continue;
					}

					nextOffset++;
					
					// is this a field name or a value? A field name must be followed by a colon
					
					// special case: no whitespace
					if(chars[nextOffset] == ':') {
						// key
						offset = nextOffset + 1;
						continue;
					}
					// most likely there is now no whitespace, but a comma, end array or end object
					
					// legal whitespaces are:
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					int quoteIndex = nextOffset;
					if(chars[nextOffset] <= 0x20) {
						// fast-forward over whitespace
						// optimization: scan for highest value
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// was a field name
							offset = nextOffset + 1;
							continue;
						}
					}
					
					if(offset + maxStringLength > maxSizeLimit) {
						offset = nextOffset;
						
						break loop;
					}
					
					int removedLength = filter.getRemovedLength();

					if(filter.addMaxLength(chars, offset + maxStringLength - 1, quoteIndex - 1, -(offset + maxStringLength - quoteIndex))) {

						// increment limit since we removed something
						maxSizeLimit += filter.getRemovedLength() - removedLength;

						if(nextOffset > maxSizeLimit) {
							// corner case, even with removal we are over the limit
							maxSizeLimit -= filter.getRemovedLength() - removedLength;
							
							filter.removeLastFilter();
							
							offset = nextOffset;
							
							break loop;
						}
						
						if(maxSizeLimit >= maxReadLimit) {
							// filter only for max string length
							filter.setLevel(0);
							
							return ranges(chars, nextOffset, maxReadLimit, maxStringLength, filter);
						}
						
						mark = nextOffset;
					}
					
					offset = nextOffset;
					
					continue;
				default :
			}
			offset++;
		}
		
		filter.setLevel(bracketLevel);
		filter.setMark(mark);
		filter.setMaxSizeLimit(maxSizeLimit);
		
		return offset;
	}
	


}
