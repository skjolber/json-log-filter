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
import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.FlexibleOutputStream;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
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
		
		int bufferLength = buffer.length();

		int maxSizeLimit = offset + maxSize;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			process(chars, offset, offset, buffer, maxReadLimit, maxSizeLimit, 0, squareBrackets, mark, writtenMark, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				if(written < length) {
					metrics.onMaxSize(length - written);
				}
				
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public static void process(final char[] chars, int offset, int flushedOffset, final StringBuilder buffer, int maxReadLimit, int maxSizeLimit, int bracketLevel, boolean[] squareBrackets, int mark, int streamMark, JsonFilterMetrics metrics) {
		loop:
		while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(flushedOffset <= mark) {
					streamMark = buffer.length() + mark - flushedOffset; 
				}
				
				// skip this char and any other whitespace
				buffer.append(chars, flushedOffset, offset - flushedOffset);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

				flushedOffset = offset;
				
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

					squareBrackets[bracketLevel] = c == '[';
					
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
					offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
					continue;
				default : {
					// some kind of value
					// do nothing
				}
			}
			offset++;
		}				

		if(bracketLevel > 0) {
			if(flushedOffset <= mark) {
				streamMark = buffer.length() + mark - flushedOffset; 
			}
			buffer.append(chars, flushedOffset, offset - flushedOffset);
			flushedOffset = offset;
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						buffer.append(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				}
				buffer.setLength(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		} else {
			buffer.append(chars, flushedOffset, offset - flushedOffset);
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}

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
			
			process(chars, offset, offset, stream, maxSizeLimit, maxReadLimit, level, squareBrackets, mark, writtenMark, metrics);
			
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
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public static void process(byte[] chars, int offset, int flushedOffset, FlexibleOutputStream stream, int maxSizeLimit, int maxReadLimit, int bracketLevel, boolean[] squareBrackets, int mark, int streamMark, JsonFilterMetrics metrics) throws IOException {
		loop:
		while(offset < maxSizeLimit) {
			
			byte c = chars[offset];
			if(c <= 0x20) {
				if(flushedOffset <= mark) {
					streamMark = stream.size() + mark - flushedOffset; 
				}
				// skip this char and any other whitespace
				stream.write(chars, flushedOffset, offset - flushedOffset);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
			
				flushedOffset = offset;

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

					squareBrackets[bracketLevel] = c == '[';
					
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
					offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
					continue;
				default : // do nothing
			}
			offset++;
		}

		if(bracketLevel > 0) {
			if(flushedOffset <= mark) {
				streamMark = stream.size() + mark - flushedOffset; 
			}
			stream.write(chars, flushedOffset, offset - flushedOffset);
			flushedOffset = offset;
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						stream.write(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				}
				stream.setCount(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, stream);
		} else {
			stream.write(chars, flushedOffset, offset - flushedOffset);
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
