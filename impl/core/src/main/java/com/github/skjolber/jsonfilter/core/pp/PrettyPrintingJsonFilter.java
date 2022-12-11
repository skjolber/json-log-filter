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
package com.github.skjolber.jsonfilter.core.pp;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class PrettyPrintingJsonFilter extends AbstractJsonFilter {
	
	private final Indent indent;
	
	public PrettyPrintingJsonFilter(Indent indent) {
		super(-1, -1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		this.indent = indent;
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics filterMetrics) {	
		length += offset;
		
		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		try {
			while(offset < length) {
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
						
						break;
					case '}' :
					case ']' :
						level--;

						indent.append(buffer, level);

						break;
					case '"' :
						do {
							buffer.append(chars[offset]);
							offset++;
						} while(chars[offset] != '"' || chars[offset - 1] == '\\');
						buffer.append(chars[offset]);
						offset++;
						
						continue;
						
					default : // do nothing
				}
				
				if(chars[offset] > 0x20) {
					buffer.append(chars[offset]);
				}

				switch(chars[offset]) {
				case ',' :
					indent.append(buffer, level);
					break;
				case ':' :
					buffer.append(' ');
					break;
				case '[' :
					int nextOffset = offset + 1;
					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(chars[nextOffset] == ']') {
						buffer.append(']');
						offset = nextOffset + 1;
						level--;
						continue;
					}
				case '{' :
					indent.append(buffer, level);
				}
				offset++;
			}
		
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
		length += offset;

		int level = 0;
		
		boolean[] squareBrackets = new boolean[32];

		try {
			while(offset < length) {
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
						
						break;
					case '}' :
					case ']' :
						level--;

						indent.append(output, level);

						break;
					case '"' :
						do {
							output.write(chars[offset]);
							offset++;
						} while(chars[offset] != '"' || chars[offset - 1] == '\\');
						output.write(chars[offset]);
						offset++;
						
						continue;
						
					default : // do nothing
				}
				
				if(chars[offset] > 0x20) {
					output.write(chars[offset]);
				}

				switch(chars[offset]) {
				case ',' :
					indent.append(output, level);
					break;
				case ':' :
					output.write(' ');
					break;
				case '[' :
					int nextOffset = offset + 1;
					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					if(chars[nextOffset] == ']') {
						output.write(']');
						offset = nextOffset + 1;
						level--;
						continue;
					}
				case '{' :
					indent.append(output, level);
				}		
				
				offset++;
			}
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		return process(chars, offset, length, output, null);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
}
