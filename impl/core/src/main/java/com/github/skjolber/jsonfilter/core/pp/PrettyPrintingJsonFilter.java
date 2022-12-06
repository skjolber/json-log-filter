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

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class PrettyPrintingJsonFilter implements JsonFilter {
	
	private final Indent indent;
	
	public PrettyPrintingJsonFilter(Indent indent) {
		this.indent = indent;
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {	
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
			
			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return false;
			}
		
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
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
			
			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return false;
			}
		
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public String process(char[] chars) {
		StringBuilder buffer = new StringBuilder(chars.length * 2);
		if(process(chars, 0, chars.length, buffer)) {
			return buffer.toString();
		}
		return null;
	}

	@Override
	public String process(String chars) {
		return process(chars.toCharArray());
	}

	@Override
	public boolean process(String chars, StringBuilder buffer) {
		return process(chars.toCharArray(), 0, chars.length(), buffer);
	}

	@Override
	public byte[] process(byte[] chars) {
		return process(chars, 0, chars.length);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		ByteArrayOutputStream output = new ByteArrayOutputStream(chars.length * 2);
		if(process(chars, 0, chars.length, output)) {
			return output.toByteArray();
		}
		return null;
	}
	
	@Override
	public String process(char[] chars, JsonFilterMetrics metrics) {
		StringBuilder buffer = new StringBuilder(chars.length * 2);
		if(process(chars, 0, chars.length, buffer)) {
			return buffer.toString();
		}
		return null;
	}

	@Override
	public String process(String chars, JsonFilterMetrics metrics) {
		return process(chars.toCharArray());
	}

	@Override
	public boolean process(String chars, StringBuilder buffer, JsonFilterMetrics metrics) {
		return process(chars.toCharArray(), 0, chars.length(), buffer);
	}

	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics metrics) {
		return process(chars, 0, chars.length);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics metrics) {
		ByteArrayOutputStream output = new ByteArrayOutputStream(chars.length * 2);
		if(process(chars, 0, chars.length, output)) {
			return output.toByteArray();
		}
		return null;
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}
}
