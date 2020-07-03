/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.RangesJsonFilter;

public class MaxStringLengthJsonFilter extends AbstractJsonFilter implements RangesJsonFilter {

	public MaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		CharArrayRangesFilter filter = new CharArrayRangesFilter(-1);

		try {
			return ranges(chars, offset, offset + length, maxStringLength, filter);
		} catch(Exception e) {
			return null;
		}
	}

	public static CharArrayRangesFilter ranges(final char[] chars, int offset, int limit, int maxStringLength, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] == '"') {
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
					} else {
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						if(chars[nextOffset] > 0x20) {
							// was a value
							filter.add(offset + maxStringLength - 1, nextOffset - 1, offset + maxStringLength - nextOffset);
							offset = nextOffset;
						} else {
							// fast-forward over whitespace
							
							// optimization: scan for highest value

							int end = nextOffset;
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);

							if(chars[nextOffset] == ':') {
								// was a key
								offset = nextOffset + 1;
							} else {
								// value
								filter.add(offset + maxStringLength - 1, end - 1, offset + maxStringLength - end);
								offset = nextOffset;
							}
						}
					}
				} else {
					offset = nextOffset;
				}
			} else {
				offset++;
			}
		}				

		if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
			return null;
		}

		return filter;
	}
}
