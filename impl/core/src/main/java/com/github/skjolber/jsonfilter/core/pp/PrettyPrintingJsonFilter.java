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

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class PrettyPrintingJsonFilter extends AbstractJsonFilter {
	
	private final Indent indent;
	
	public PrettyPrintingJsonFilter(Indent indent) {
		super(-1, -1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		this.indent = indent;
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics filterMetrics) {
		int bufferLength = buffer.length();
		if(prettyPrint(chars, offset, length, buffer)) {
			if(filterMetrics != null) {
				filterMetrics.onInput(length);
				filterMetrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		}
		// do not leave partial output in the buffer
		buffer.setLength(bufferLength);
		return false;
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
		int bufferLength = output.size();
		if(prettyPrint(chars, offset, length, output)) {
			if(filterMetrics != null) {
				filterMetrics.onInput(length);
				filterMetrics.onOutput(output.size() - bufferLength);
			}
			return true;
		}
		// do not leave partial output in the buffer
		output.setSize(bufferLength);
		return false;
	}

	// keep the loop free of other locals, these slow it down considerably
	private boolean prettyPrint(final char[] chars, int offset, int length, final StringBuilder buffer) {
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
						int quoteEnd = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
						buffer.append(chars, offset, quoteEnd - offset);
						offset = quoteEnd;
						
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
				case '{' :
					int nextOffset = offset + 1;
					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					// empty array or object: keep it on one line
					char end = chars[offset] == '[' ? ']' : '}';
					if(chars[nextOffset] == end) {
						buffer.append(end);
						offset = nextOffset + 1;
						level--;
						continue;
					}
					indent.append(buffer, level);
					// the whitespace up to nextOffset is dropped anyway
					offset = nextOffset;
					continue;
				}
				offset++;
			}
		
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	private boolean prettyPrint(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
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
						int quoteEnd = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
						output.write(chars, offset, quoteEnd - offset);
						offset = quoteEnd;
						
						continue;
						
					default : // do nothing
				}
				
				if(chars[offset] > 0x20 || chars[offset] < 0) {
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
				case '{' :
					int nextOffset = offset + 1;
					// skip whitespace
					// optimization: scan for highest value
					while(0 <= chars[nextOffset] && chars[nextOffset] <= 0x20) {
						nextOffset++;
					}
					
					// empty array or object: keep it on one line
					byte end = chars[offset] == '[' ? (byte)']' : (byte)'}';
					if(chars[nextOffset] == end) {
						output.write(end);
						offset = nextOffset + 1;
						level--;
						continue;
					}
					indent.append(output, level);
					// the whitespace up to nextOffset is dropped anyway
					offset = nextOffset;
					continue;
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
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
}
