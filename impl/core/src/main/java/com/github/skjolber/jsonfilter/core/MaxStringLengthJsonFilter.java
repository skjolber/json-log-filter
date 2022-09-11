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

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class MaxStringLengthJsonFilter extends AbstractRangesJsonFilter {

	public MaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	protected MaxStringLengthJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	protected CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		CharArrayRangesFilter filter = getCharArrayRangesFilter(length);
		
		int limit = offset + length;
		try {
			offset = ranges(chars, offset, limit, maxStringLength, filter);
			if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}
			return filter;			
		} catch(Exception e) {
			return null;
		}
	}

	protected ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int maxStringLength = this.maxStringLength + 2; // account for quotes
		
		ByteArrayRangesFilter filter = getByteArrayRangesFilter(length);

		int limit = offset + length;
		try {
			offset = ranges(chars, offset, limit, maxStringLength, filter);
			if(offset > limit) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}
			return filter;			
		} catch(Exception e) {
			return null;
		}
	}
	
	public static int ranges(final char[] chars, int offset, int limit, int maxStringLength, CharArrayRangesFilter filter) {
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
						
						continue;
					} else {
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						if(chars[nextOffset] > 0x20) {
							// was a value
							filter.addMaxLength(chars, offset + maxStringLength - 1, nextOffset - 1, -(offset + maxStringLength - nextOffset));
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
								
								continue;
							} else {
								// value
								filter.addMaxLength(chars, offset + maxStringLength - 1, end - 1, -(offset + maxStringLength - end));
							}
						}
					}
				}
				offset = nextOffset;
			} else {
				offset++;
			}
		}				
		return offset;
	}
	
	public static int ranges(final byte[] chars, int offset, int limit, int maxStringLength, ByteArrayRangesFilter filter) {
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
							filter.addMaxLength(chars, offset + maxStringLength - 1, nextOffset - 1, -(offset + maxStringLength - nextOffset));
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
								filter.addMaxLength(chars, offset + maxStringLength - 1, end - 1, -(offset + maxStringLength - end));
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

		return offset;
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
