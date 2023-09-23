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
import com.github.skjolber.jsonfilter.base.FlexibleOutputStream;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class MaxSizeRemoveWhitespaceJsonFilter extends RemoveWhitespaceJsonFilter {

	public MaxSizeRemoveWhitespaceJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizeRemoveWhitespaceJsonFilter(int maxSize) {
		this(maxSize, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, buffer, metrics);
		}
		
		int startOffset = offset;
		
		int bufferLength = buffer.length();

		int maxSizeLimit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			int writtenOffset = offset;
			
			loop:
			while(offset < maxSizeLimit) {
				char c = chars[offset];
				if(c <= 0x20) {
					if(writtenOffset <= mark) {
						writtenMark = buffer.length() + mark - writtenOffset; 
					}
					
					// skip this char and any other whitespace
					buffer.append(chars, writtenOffset, offset - writtenOffset);
					do {
						offset++;
						maxSizeLimit++;
					} while(chars[offset] <= 0x20);

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}

					writtenOffset = offset;
					
					if(offset >= maxSizeLimit) {
						break loop;
					}
					c = chars[offset];
				}

				switch(c) {
					case '{' :
					case '[' :
						// check corner case
						maxSizeLimit--;
						if(offset >= maxSizeLimit) {
							break loop;
						}
	
						squareBrackets[level] = c == '[';
						
						level++;
						if(level >= squareBrackets.length) {
							boolean[] next = new boolean[squareBrackets.length + 32];
							System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
							squareBrackets = next;
						}
						
						offset++;
						mark = offset;
	
						continue;
					case '}' :
					case ']' :
						level--;
						maxSizeLimit++;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						offset++;
						mark = offset;
	
						continue;
					case ',' :
						mark = offset;
						break;
					case '"' :
						// avoid escaped double quotes
						// also avoid to count escaped double slash an escape character
						do {
							if(chars[offset] == '\\') {
								offset++;
							}
							offset++;
						} while(chars[offset] != '"');
					default : {
						// some kind of value
						// do nothing
					}
				}
				offset++;
			}				
		
			if(level > 0) {
				
				markLimit:
				if(mark <= maxSizeLimit) {
					int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
					if(markLimit <= maxSizeLimit) {
						if(markLimit >= writtenOffset) {
							buffer.append(chars, writtenOffset, markLimit - writtenOffset);
							break markLimit;
						}
					}
					buffer.setLength(writtenMark);
				}
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, buffer);

				if(metrics != null) {
					metrics.onInput(length);
					int written = buffer.length() - bufferLength;
					if(written < length) {
						metrics.onMaxSize(length - written);
					}
					
					metrics.onOutput(buffer.length() - bufferLength);
				}
			} else {
				buffer.append(chars, writtenOffset, offset - writtenOffset);

				if(metrics != null) {
					metrics.onInput(length);
					metrics.onOutput(buffer.length() - bufferLength);
				}
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}

		int startOffset = offset;
		
		int bufferLength = output.size();
		
		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);
		
		int maxSizeLimit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			int writtenOffset = offset;

			loop:
			while(offset < maxSizeLimit) {
				
				byte c = chars[offset];
				if(c <= 0x20) {
					if(writtenOffset <= mark) {
						writtenMark = stream.size() + mark - writtenOffset; 
					}
					// skip this char and any other whitespace
					stream.write(chars, writtenOffset, offset - writtenOffset);
					do {
						offset++;
						maxSizeLimit++;
					} while(chars[offset] <= 0x20);

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				
					writtenOffset = offset;

					if(offset >= maxSizeLimit) {
						break loop;
					}
					c = chars[offset];
				}
				
				switch(c) {
					case '{' :
					case '[' :
						// check corner case
						maxSizeLimit--;
						if(offset >= maxSizeLimit) {
							break loop;
						}
	
						squareBrackets[level] = c == '[';
						
						level++;
						if(level >= squareBrackets.length) {
							boolean[] next = new boolean[squareBrackets.length + 32];
							System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
							squareBrackets = next;
						}
						
						offset++;
						mark = offset;
						
						continue;
					case '}' :
					case ']' :
						level--;
						maxSizeLimit++;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						offset++;
						mark = offset;
						
						continue;
					case ',' :
						mark = offset;
						break;
					case '"' :
						// avoid escaped double quotes
						// also avoid to count escaped double slash an escape character
						do {
							if(chars[offset] == '\\') {
								offset++;
							}
							offset++;
						} while(chars[offset] != '"');
					default : // do nothing
				}
				offset++;
			}

			if(level > 0) {
				markLimit:
				if(mark <= maxSizeLimit) {
					int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
					if(markLimit <= maxSizeLimit) {
						if(markLimit >= writtenOffset) {
							stream.write(chars, writtenOffset, markLimit - writtenOffset);
							break markLimit;
						}
					}
					stream.setCount(writtenMark);
				}
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, stream);
				
				stream.writeTo(output);
				
				if(metrics != null) {
					metrics.onInput(length);
					int written = output.size() - bufferLength;
					int totalSize = length;
					if(written < totalSize) {
						metrics.onMaxSize(totalSize - totalSize);
					}					
					metrics.onOutput(output.size() - bufferLength);
				}
			} else {
				stream.write(chars, writtenOffset, offset - writtenOffset);
				stream.writeTo(output);
				
				if(metrics != null) {
					metrics.onInput(length);
					metrics.onOutput(output.size() - bufferLength);
				}
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
