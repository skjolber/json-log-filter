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

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

/**
 * 
 * A special filter which also removes newlines in string values.
 * 
 */

public class PrettyPrintNewlineStringJsonFilter extends AbstractJsonFilter {

	public PrettyPrintNewlineStringJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public PrettyPrintNewlineStringJsonFilter() {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {	
		length += offset;
		
		try {
			int start = offset;
			
			while(offset < length) {
				char c = chars[offset];
				if(c == '"') {
					do {
						// tight inner loop for skipping normal chars. 
						// the loop will exit on regular space :-(
						do {
							offset++;
						} while(chars[offset] > '"');
						
						if(chars[offset] == '\n') {
							buffer.append(chars, start, offset - start); // not including last char (newline)
							buffer.append(' '); // convert newline to space
							start = offset + 1;
							continue;
						}
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					buffer.append(chars, start, offset - start);
					
					start = offset;
					
					continue;
				} else if(c <= 0x20) {
					// skip this char and any other whitespace
					buffer.append(chars, start, offset - start);
					do {
						offset++;
					} while(chars[offset] <= 0x20);
					
					start = offset;
					
					continue;
				}
				offset++;
			}
			buffer.append(chars, start, offset - start);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		length += offset;
		
		try {
			int start = offset;
			
			while(offset < length) {
				byte c = chars[offset];
				if(c == '"') {
					do {
						// tight inner loop for skipping normal chars. 
						// the loop will exit on regular space :-(
						do {
							offset++;
						} while(chars[offset] > '"');
						
						if(chars[offset] == '\n') {
							output.write(chars, start, offset - start); // not including last char (newline)
							output.write(' '); // convert newline to space
							start = offset + 1;
							continue;
						}
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					output.write(chars, start, offset - start);
					
					start = offset;
					
					continue;
				} else if(c <= 0x20) {
					// skip this char and any other whitespace
					output.write(chars, start, offset - start);
					do {
						offset++;
					} while(chars[offset] <= 0x20);
					
					start = offset;
					
					continue;
				}
				offset++;
			}
			output.write(chars, start, offset - start);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
}
