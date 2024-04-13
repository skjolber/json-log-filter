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

import java.io.IOException;
import java.io.OutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class MaxSizeJsonFilter extends AbstractJsonFilter {

	public MaxSizeJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizeJsonFilter(int maxSize) {
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

		int bufferLength = buffer.length();
		
		try {
			processMaxSize(chars, offset, length, offset + maxSize, buffer);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			return false;
		}			
	}

	protected static void processMaxSize(final char[] chars, int offset, int length, int maxSizeLimit, final StringBuilder buffer) {
		int flushOffset = offset;
		
		int bracketLevel = 0;
		
		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

			loop:
			while(offset < maxSizeLimit) {
				switch(chars[offset]) {
					case '{' :
					case '[' :
						// check corner case
						maxSizeLimit--;
						if(offset >= maxSizeLimit) {
							break loop;
						}

						squareBrackets[bracketLevel] = chars[offset] == '[';
						
						bracketLevel++;
						if(bracketLevel >= squareBrackets.length) {
							boolean[] next = new boolean[squareBrackets.length + 32];
							System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
							squareBrackets = next;
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
						offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
						continue;
						
					default:
				}
				offset++;
			}
			
			if(bracketLevel > 0) {
				int markLimit = markToLimit(chars, offset, flushOffset + length, maxSizeLimit, mark);
				if(markLimit != -1) {
					buffer.append(chars, flushOffset, markLimit - flushOffset);
				} else {
					buffer.append(chars, flushOffset, mark - flushOffset);
				}
				
				closeStructure(bracketLevel, squareBrackets, buffer);
			} else {
				buffer.append(chars, flushOffset, offset - flushOffset);				
			}
	}

	public static int markToLimit(final char[] chars, int offset, int maxReadLimit, int maxSizeLimit, int mark) {
		// see whether we can include the last value,
		// which might otherwise be excluded by size exceeded due to whitespace
		// or as a corner case. 
		// note: offset is never in the middle of a field name or string value
		//
		// | Overview:                                                 
		// |-----------------------------------------------------------|
		// | [\n  {\n    "key" : [\n      "a"\n    ]\n  }\n]           |
		// |                     ^           ^   ^                     | 
		// |                mark ╯           |   |                     |
		// |                    desired mark ╯   ╰  max reached        |
		// |-----------------------------------------------------------|
		
		int previousOffset = offset - 1;
		
		while(mark < previousOffset) {
			if(chars[previousOffset] > 0x20) {
				if(previousOffset < maxSizeLimit) {
					// check if there is a proper terminator
					// after the current offset
					
					int nextOffset = offset;
					
					while(nextOffset < maxReadLimit) {
						if(chars[nextOffset] > 0x20) {
							switch(chars[nextOffset]) {
							case ',':
							case ']':
							case '}': {
								// chars[nextOffset] must be a value of some sorts
								return previousOffset + 1;
							}
							default : {
								// do nothing
							}
							
							}
							break;
						}
						nextOffset++;
					}									
				}
				
				break;
			}
			previousOffset--;
		}
		return -1;
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {		
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
		
		int bufferLength = output.size();
		
		try {
			processMaxSize(chars, offset, length, offset + maxSize, output);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = output.size() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(output.size() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	private static void processMaxSize(byte[] chars, int offset, int length, int maxSizeLimit, ResizableByteArrayOutputStream output) throws IOException {
		int flushOffset = offset;

		int bracketLevel = 0;
		
		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
					// check corner case
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}

					squareBrackets[bracketLevel] = chars[offset] == '[';
					
					bracketLevel++;
					if(bracketLevel >= squareBrackets.length) {
						boolean[] next = new boolean[squareBrackets.length + 32];
						System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
						squareBrackets = next;
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
					offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
					continue;
				default : // do nothing
			}
			offset++;
		}

		if(bracketLevel > 0) {
			int markLimit = markToLimit(chars, offset, flushOffset + length, maxSizeLimit, mark);
			if(markLimit != -1) {
				output.write(chars, flushOffset, markLimit - flushOffset);
			} else {
				output.write(chars, flushOffset, mark - flushOffset);
			}
			
			closeStructure(bracketLevel, squareBrackets, output);
		} else {
			output.write(chars, flushOffset, offset - flushOffset);
		}
	}

	public static int markToLimit(byte[] chars, int offset, int maxReadLimit, int maxSizeLimit, int mark) {
		// see whether we can include the last value,
		// which might otherwise be excluded by size exceeded due to whitespace
		// or as a corner case. 
		// note: offset is never in the middle of a field name or string value
		//
		// | Overview:                                                 
		// |-----------------------------------------------------------|
		// | [\n  {\n    "key" : [\n      "a"\n    ]\n  }\n]           |
		// |                     ^           ^   ^                     | 
		// |                mark ╯           |   |                     |
		// |                    desired mark ╯   ╰  max reached        |
		// |-----------------------------------------------------------|
		
		int previousOffset = offset - 1;
		
		while(mark < previousOffset) {
			if(chars[previousOffset] > 0x20 || chars[previousOffset] < 0) { // note: negative because could be UTF-8 multi characther
				if(previousOffset < maxSizeLimit) {
					// check if there is a proper terminator
					// after the current offset
					
					int nextOffset = offset;
					
					while(nextOffset < maxReadLimit) {
						if(chars[nextOffset] > 0x20 || chars[previousOffset] < 0) {
							switch(chars[nextOffset]) {
							case ',':
							case ']':
							case '}': {
								// chars[nextOffset] must be a value of some sorts
								return previousOffset + 1;
							}
							default : {
								// do nothing
							}
							
							}
							break;
						}
						nextOffset++;
					}									
				}
				
				break;
			}
			previousOffset--;
		}
		return -1;
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
