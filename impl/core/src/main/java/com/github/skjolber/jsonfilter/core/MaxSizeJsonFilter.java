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

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class MaxSizeJsonFilter extends AbstractJsonFilter {

	public MaxSizeJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, pruneMessage, anonymizeMessage, truncateMessage, maxSize);
	}

	public MaxSizeJsonFilter(int maxSize) {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, maxSize);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		if(length <= maxSize) {
			buffer.append(chars, offset, length);
			return true;
		}
		
		length = offset + maxSize;

		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		length += offset;
		
		int mark = 0;

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
					case '[' :
						squareBrackets[level] = chars[offset] == '[';
						
						level++;
						if(level >= squareBrackets.length) {
							boolean[] next = new boolean[squareBrackets.length + 3];
							System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
							squareBrackets = next;
						}
						mark = offset;
						
						break;
					case '}' :
					case ']' :
						level--;
					case ',' :
						mark = offset;
						break;
					case '"' :					
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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
						}

						offset = nextOffset;
						
						continue;
						
					default : // do nothing
				}
				offset++;
				
			}
			switch(chars[mark]) {
			
				case '{' :
				case '}' :
				case '[' :
				case ']' :
					mark++;
	
					break;
				default : // do nothing
			}
			
			buffer.append(chars, 0, mark);
			
			if(level > 0) {
				for(int i = level - 1; i >= 0; i--) {
					if(squareBrackets[i]) {
						buffer.append(']');
					} else {
						buffer.append('}');
					}
				}
			}

			return true;
		} catch(Exception e) {
			return false;
		}

	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		if(length <= maxSize) {
			output.write(chars, offset, length);
			return true;
		}
		
		length = offset + maxSize;

		int level = 0;
		
		boolean[] squareBrackes = new boolean[32];

		length += offset;
		
		int mark = 0;

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
					case '[' :
						squareBrackes[level] = chars[offset] == '[';
						
						level++;
						if(level >= squareBrackes.length) {
							boolean[] next = new boolean[squareBrackes.length + 3];
							System.arraycopy(squareBrackes, 0, next, 0, squareBrackes.length);
							squareBrackes = next;
						}
						mark = offset;
						
						break;
					case '}' :
					case ']' :
						level--;
					case ',' :
						mark = offset;
						break;
					case '"' :					
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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
						}

						offset = nextOffset;
						
						continue;
						
					default : // do nothing
				}
				offset++;
				
			}
			switch(chars[mark]) {
			
				case '{' :
				case '}' :
				case '[' :
				case ']' :
					mark++;
	
					break;
				default : // do nothing
			}
			
			output.write(chars, 0, mark);
			
			if(level > 0) {
				for(int i = level - 1; i >= 0; i--) {
					if(squareBrackes[i]) {
						output.write(']');
					} else {
						output.write('}');
					}
				}
			}

			return true;
		} catch(Exception e) {
			return false;
		}

	}
}
