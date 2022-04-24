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

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class MaxSizeJsonFilter extends AbstractJsonFilter {

	public MaxSizeJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizeJsonFilter(int maxSize) {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, maxSize);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {	
		if(!mustConstrainMaxSize(length)) {
			if(chars.length < offset + length) {
				return false;
			}
			buffer.append(chars, offset, length);
			return true;
		}
		
		length += offset;
		
		int limit = offset + maxSize; // i.e. now limit

		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		try {
			while(offset < limit) {
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
			
			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return false;
			}
			
			mark = alignMark(mark, chars);
			
			buffer.append(chars, 0, mark);
			
			closeStructure(level, squareBrackets, buffer);
		
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		if(!mustConstrainMaxSize(length)) {
			if(chars.length < offset + length) {
				return false;
			}
			output.write(chars, offset, length);
			
			return true;
		}
		
		length += offset;
		
		int limit = offset + maxSize; // i.e. now limit

		int level = 0;
		
		boolean[] squareBrackes = new boolean[32];

		int mark = 0;

		try {
			while(offset < limit) {
				switch(chars[offset]) {
					case '{' :
					case '[' :
						squareBrackes[level] = chars[offset] == '[';
						
						level++;
						if(level >= squareBrackes.length) {
							boolean[] next = new boolean[squareBrackes.length + 32];
							System.arraycopy(squareBrackes, 0, next, 0, squareBrackes.length);
							squareBrackes = next;
						}
						mark = offset;
						
						break;
					case '}' :
					case ']' :
						level--;
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
			
			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return false;
			}

			mark = alignMark(mark, chars);
			
			output.write(chars, 0, mark);
			
			closeStructure(level, squareBrackes, output);
	
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	private void closeStructure(int level, boolean[] squareBrackets, final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
	}
	
	public int alignMark(int mark, byte[] chars) {
		switch(chars[mark]) {
			
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
	
	public int alignMark(int mark, char[] chars) {
		switch(chars[mark]) {
			
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
	
	private void closeStructure(int level, boolean[] squareBrackes, OutputStream output) throws IOException {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackes[i]) {
				output.write(']');
			} else {
				output.write('}');
			}
		}
	}
}
