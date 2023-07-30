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
import java.io.IOException;
import java.io.OutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class MaxSizeJsonFilter2 extends AbstractJsonFilter {

	public MaxSizeJsonFilter2(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizeJsonFilter2(int maxSize) {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, maxSize);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {	
		return process(chars, offset, length, buffer, null);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {	
		if(!mustConstrainMaxSize(length)) {
			if(chars.length < offset + length) {
				return false;
			}
			buffer.append(chars, offset, length);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(length);
			}
			
			return true;
		}

		int startOffset = offset;
		
		int bufferLength = buffer.length();
		
		int maxReadLimit = offset + maxSize;
		
		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		boolean object = true;
		
		try {
			while(offset < maxReadLimit) {
				switch(chars[offset]) {
					case '{' :
					case '[' :
						object = squareBrackets[level] = chars[offset] == '[';
						
						level++;
						if(level >= squareBrackets.length) {
							boolean[] next = new boolean[squareBrackets.length + 32];
							System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
							squareBrackets = next;
						}
						mark = offset;
						
						maxReadLimit--;
						
						break;
					case '}' :
					case ']' :
						level--;
						maxReadLimit++;
						
						object = chars[offset] == ']';
						// fall through
					case ',' :
						mark = offset;
						break;
					case '"' :
						do {
							offset++;
						} while(chars[offset] != '"' || chars[offset - 1] == '\\');
						int nextOffset = offset + 1;
						
						// is this a field name or a value? A field name must be followed by a colon
						
						// special case: no whitespace
						if(chars[nextOffset] == ':') {
							// key, no mark
							offset = nextOffset + 1;
							
							continue;
						} 
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						};
						
						if(chars[nextOffset] == ':') {
							// key, no mark
							offset = nextOffset + 1;
							
							continue;
						}						

						// value, mark here
						mark = offset + 1;
						offset = nextOffset;
						
						continue;
						
					// whitespace
					case ' ':
					case '\t':
					case '\n':
					case '\r':
						break;

					case 't': {
						// true
						offset += 3;
						mark = offset;						
						break;
					}
					case 'f': {
						// false
						offset += 4;
						mark = offset;
						break;
					}
					case 'n': {
						// null
						offset += 3;
						mark = offset;
						break;
					}						

					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
					case '-': {
						// number

						loop:
						do {
							offset++;
							switch(chars[offset]) {
								case '0':
								case '1':
								case '2':
								case '3':
								case '4':
								case '5':
								case '6':
								case '7':
								case '8':
								case '9':
								case '+': 
								case '-': 
								case '.':
								case 'e':
								case 'E':
									break;
								default: break loop;
							}
							offset++;
						} while(true);
						
						
					}
					
					default : {
						// do nothing
					}
				}
				offset++;
			}
			
			int markLimit = markToLimit(mark, chars);
			
			buffer.append(chars, startOffset, markLimit - startOffset);
			
			closeStructure(level, squareBrackets, buffer);
		
			if(metrics != null) {
				metrics.onInput(length);
				int charsLimit = startOffset + length;
				if(markLimit < charsLimit) {
					metrics.onMaxSize(charsLimit - markLimit);
				}
				metrics.onOutput(buffer.length() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {		
		if(!mustConstrainMaxSize(length)) {
			if(chars.length < offset + length) {
				return false;
			}
			output.write(chars, offset, length);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(length);
			}
			
			return true;
		}
		
		int startOffset = offset;

		int bufferLength = output.size();
		
		int maxReadLimit = offset + maxSize;

		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		try {
			while(offset < maxReadLimit) {
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
						
						maxReadLimit--;
						
						break;
					case '}' :
					case ']' :
						level--;
						maxReadLimit++;
						// fall through
					case ',' :
						mark = offset;
						break;
					case '"' :
						do {
							offset++;
						} while(chars[offset] != '"' || chars[offset - 1] == '\\');
						int nextOffset = offset + 1;
						
						// is this a field name or a value? A field name must be followed by a colon
						
						// special case: no whitespace
						if(chars[nextOffset] == ':') {
							// key, no mark
							offset = nextOffset + 1;
							
							continue;
						} 
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						};
						
						if(chars[nextOffset] == ':') {
							// key, no mark
							offset = nextOffset + 1;
							
							continue;
						}						

						// value, mark here
						mark = offset + 1;
						offset = nextOffset;
						
						continue;
						
					default : // do nothing
				}
				offset++;
				
			}

			int markLimit = markToLimit(mark, chars);
			
			output.write(chars, startOffset, markLimit - startOffset);
			
			closeStructure(level, squareBrackets, output);
			
			if(metrics != null) {
				metrics.onInput(length);
				int charsLimit = startOffset + length;
				if(markLimit < charsLimit) {
					metrics.onMaxSize(charsLimit - markLimit);
				}
				metrics.onOutput(output.size() - bufferLength);
			}
	
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public static void closeStructure(int level, boolean[] squareBrackets, final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
	}
	
	public static int markToLimit(int mark, char c) {
		switch(c) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return mark + 1;
			default : {
				return mark;
			}
		}
	}
	
	public static int markToLimit(int mark, byte c) {
		switch(c) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return mark + 1;
			default : {
				return mark;
			}
		}
	}
	
	public static int markToLimit(int mark, byte[] chars) {
		return markToLimit(mark, chars[mark]);
	}
	
	public static int markToLimit(int mark, char[] chars) {
		return markToLimit(mark, chars[mark]);
	}
	
	public static void closeStructure(int level, boolean[] squareBrackets, OutputStream output) throws IOException {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				output.write(']');
			} else {
				output.write('}');
			}
		}
	}
}
