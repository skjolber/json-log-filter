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

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

/**
 * 
 * A special filter which also removes newlines in string values.
 * 
 */

public class RemoveWhitespaceNewlineStringJsonFilter extends AbstractJsonFilter {

	public RemoveWhitespaceNewlineStringJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public RemoveWhitespaceNewlineStringJsonFilter() {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {	
		int bufferLength = buffer.length();
		
		try {
			int limit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			int flushOffset = offset;
			
			while(offset < limit) {
				char c = chars[offset];
				if(c == '"') {
					do {
						// tight inner loop for skipping normal chars. 
						// the loop will exit on regular space :-(
						do {
							offset++;
						} while(chars[offset] > '"');
						
						if(chars[offset] == '\n') {
							buffer.append(chars, flushOffset, offset - flushOffset); // not including last char (newline)
							buffer.append(' '); // convert newline to space
							flushOffset = offset + 1;
							continue;
						}
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					buffer.append(chars, flushOffset, offset - flushOffset);
					
					flushOffset = offset;
					
					continue;
				} else if(c <= 0x20) {
					// skip this char and any other whitespace
					buffer.append(chars, flushOffset, offset - flushOffset);
					do {
						offset++;
					} while(chars[offset] <= 0x20);
					
					flushOffset = offset;
					
					continue;
				}
				offset++;
			}
			buffer.append(chars, flushOffset, offset - flushOffset);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(buffer.length() - bufferLength);
			}
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int bufferLength = output.size();
		
		try {
			int limit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			int flushOffset = offset;
			
			while(offset < limit) {
				byte c = chars[offset];
				if(c == '"') {
					do {
						// tight inner loop for skipping normal chars. 
						// the loop will exit on regular space :-(
						do {
							offset++;
						} while(chars[offset] > '"');
						
						if(chars[offset] == '\n') {
							output.write(chars, flushOffset, offset - flushOffset); // not including last char (newline)
							output.write(' '); // convert newline to space
							flushOffset = offset + 1;
							continue;
						}
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					output.write(chars, flushOffset, offset - flushOffset);
					
					flushOffset = offset;
					
					continue;
				} else if(c <= 0x20) {
					// skip this char and any other whitespace
					output.write(chars, flushOffset, offset - flushOffset);
					do {
						offset++;
					} while(chars[offset] <= 0x20);
					
					flushOffset = offset;
					
					continue;
				}
				offset++;
			}
			output.write(chars, flushOffset, offset - flushOffset);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(output.size() - bufferLength);
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
	
	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
