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

import com.github.skjolber.jsonfilter.base.BracketStructure;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesBracketFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;
public class MaxStringLengthMaxSizeJsonFilter extends MaxStringLengthJsonFilter implements RangesJsonFilter {

	public MaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthMaxSizeJsonFilter(int maxStringLength, int maxSize) {
		this(maxStringLength, maxSize, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		if(length <= maxSize) {
			return super.ranges(chars, offset, length);
		}

		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		CharArrayRangesBracketFilter filter = getCharArrayRangesBracketFilter(-1, length);

		try {
			return ranges(chars, offset, offset + length, offset + maxSize, maxStringLength, filter);
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		if(length <= maxSize) {
			return super.ranges(chars, offset, length);
		}
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		ByteArrayRangesBracketFilter filter = getByteArrayRangesBracketFilter(-1, length);

		try {
			return ranges(chars, offset, offset + length, offset + maxSize, maxStringLength, filter);
		} catch(Exception e) {
			return null;
		}
	}
	
	public static CharArrayRangesFilter ranges(final char[] chars, int offset, int limit, int maxSizeLimit, int maxStringLength, CharArrayRangesBracketFilter filter) {
		BracketStructure bracketStructure = filter.getBracketStructure(); 
		
		boolean[] squareBrackets = bracketStructure.getSquareBrackets();
		int bracketLevel = bracketStructure.getLevel();
		int mark = bracketStructure.getMark();
		
		try {
			loop:
			while(offset < maxSizeLimit) {
				switch(chars[offset]) {
					case '{' :
						squareBrackets[bracketLevel] = false;
						bracketLevel++;
						
						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = bracketStructure.grow(squareBrackets);
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
						nextOffset++;
						
						if(nextOffset - offset > maxStringLength) {
							// is this a field name or a value? A field name must be followed by a colon
							
							// special case: no whitespace
							if(chars[nextOffset] == ':') {
								// key
								offset = nextOffset + 1;
								continue;
							} else {
								// most likely there is now no whitespace, but a comma, end array or end object
								
								// legal whitespaces are:
								// space: 0x20
								// tab: 0x09
								// carriage return: 0x0D
								// newline: 0x0A

								int end = nextOffset - 1;
								if(chars[nextOffset] <= 0x20) {
									// fast-forward over whitespace
									// optimization: scan for highest value
									do {
										nextOffset++;
									} while(chars[nextOffset] <= 0x20);

									if(chars[nextOffset] == ':') {
										// was a key
										offset = nextOffset + 1;
										continue;
									}
								}
								
								if(offset + maxStringLength >= maxSizeLimit) {
									break loop;
								}
								
								int removedLength = filter.getRemovedLength();

								filter.addMaxLength(chars, offset + maxStringLength - 1, end, -(offset + maxStringLength - end - 1));

								// increment limit since we removed something
								maxSizeLimit += filter.getRemovedLength() - removedLength;
								
								if(maxSizeLimit >= limit) {
									// filter only for max string length
									bracketStructure.setLevel(0);
									
									return ranges(chars, nextOffset, limit, maxStringLength, filter); 
								}
								mark = nextOffset;
							}
						}					
						offset = nextOffset;
						continue;
						
					default :
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
				
				// filter rest of document
				filter.addDelete(bracketStructure.getMark(), limit);
			} else {
				// was able to fit the end of the document
				if(bracketLevel != 0) {
					return null;
				}
				
				bracketStructure.setLevel(0);
			}
			
			return filter;
		} catch(Exception e) {
			return null;
		}
	}
	
	public static ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length, int maxSize, int maxStringLength, ByteArrayRangesFilter filter) {
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
