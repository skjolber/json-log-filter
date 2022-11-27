/***************************************************************************
 * Copyright 2022 Thomas Rorvik Skjolberg
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
package com.github.skjolber.jsonfilter.core.ws;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharWhitespaceBracketFilter;

public class MaxSizeRemoveWhitespaceJsonFilter2 {

	public int skipObjectOrArray(final char[] chars, int offset, int maxLimit, final StringBuilder buffer, CharWhitespaceBracketFilter filter) {
		int levelLimit = filter.getLimit() - 1;

		int limit = filter.getLimit();

		int level = filter.getLevel();
		boolean[] squareBrackets = filter.getSquareBrackets();

		int mark = filter.getMark();
		int writtenMark = filter.getWrittenMark();

		int start = filter.getStart();

		loop: while(offset < limit) {
			switch(chars[offset]) {
			case '{' :
			case '[' :
				squareBrackets[level] = chars[offset] == '[';

				level++;
				if(level >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				level--;

				mark = offset;

				if(level == levelLimit) {
					offset++;
					break loop;
				}
				break;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				offset++;
				
				continue;
			}
			default : {
				if(chars[offset] <= 0x20) {
					// skip this char and any other whitespace
					if(start <= mark) {
						writtenMark = buffer.length() + mark - start; 
					}
					buffer.append(chars, start, offset - start);
					do {
						offset++;
						limit++;
					} while(offset < maxLimit && chars[offset] <= 0x20);

					if(limit >= maxLimit) {
						limit = maxLimit;
					}
					start = offset;

					continue;
				}
			}
			}
			offset++;
		}
		
		filter.setWrittenMark(writtenMark);
		filter.setStart(start);
		filter.setMark(mark);
		filter.setLevel(level);
		filter.setLimit(limit);
		
		return offset;
	}
	
	public int skipObjectOrArray(final char[] chars, int offset, int maxLimit, final StringBuilder buffer, int maxStringLength, JsonFilterMetrics metrics, CharWhitespaceBracketFilter filter) {

		int levelLimit = filter.getLimit() - 1;

		int limit = filter.getLimit();

		int level = filter.getLevel();
		boolean[] squareBrackets = filter.getSquareBrackets();

		int mark = filter.getMark();
		int writtenMark = filter.getWrittenMark();

		int start = filter.getStart();
		
		loop: while(offset < limit) {
			switch(chars[offset]) {
			case '{' :
			case '[' :
				squareBrackets[level] = chars[offset] == '[';

				level++;
				if(level >= squareBrackets.length) {
					boolean[] next = new boolean[squareBrackets.length + 32];
					System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
					squareBrackets = next;
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				level--;

				mark = offset;

				if(level == levelLimit) {
					offset++;
					break loop;
				}
				break;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

				if(nextOffset - offset - 1 > maxStringLength) {
					int endQuoteIndex = nextOffset;

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] == ':') {
						
						// was a key
						if(endQuoteIndex != nextOffset) {
							// did skip whitespace

							if(start <= mark) {
								writtenMark = buffer.length() + mark - start; 
							}
							buffer.append(chars, start, endQuoteIndex - start + 1);
							
							limit += nextOffset - endQuoteIndex;
							if(limit >= maxLimit) {
								limit = maxLimit;
							}
							
							start = nextOffset;
							offset = nextOffset;
							continue;
						}
					} else {
						// was a value
						if(start <= mark) {
							writtenMark = buffer.length() + mark - start; 
						}
						int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						buffer.append(chars, start, aligned - start);
						buffer.append(filter.getTruncateString());
						buffer.append(endQuoteIndex - aligned);
						buffer.append('"');
						
						if(metrics != null) {
							metrics.onMaxStringLength(1);
						}
						
						limit += nextOffset - aligned; // also accounts for skipped whitespace, if any
						if(limit >= maxLimit) {
							limit = maxLimit;
						}
						
						start = nextOffset;
					}
				} else {
					nextOffset++;
				}
				offset = nextOffset;

				continue;
			}
			default : {
				if(chars[offset] <= 0x20) {
					// skip this char and any other whitespace
					if(start <= mark) {
						writtenMark = buffer.length() + mark - start; 
					}
					buffer.append(chars, start, offset - start);
					do {
						offset++;
						limit++;
					} while(offset < maxLimit && chars[offset] <= 0x20);

					if(limit >= maxLimit) {
						limit = maxLimit;
					}
					start = offset;

					continue;
				}
			}
			}
			offset++;
		}
		
		filter.setWrittenMark(writtenMark);
		filter.setStart(start);
		filter.setMark(mark);
		filter.setLevel(level);
		filter.setLimit(limit);

		return offset;
	}

}
