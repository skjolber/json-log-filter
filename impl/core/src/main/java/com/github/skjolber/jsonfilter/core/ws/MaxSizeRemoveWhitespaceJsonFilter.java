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
import com.github.skjolber.jsonfilter.core.util.ByteWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharWhitespaceFilter;

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

		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

		try {
			int maxLimit = CharWhitespaceFilter.skipWhitespaceBackwards(chars, length + offset);

			int start = offset;

			while(offset < limit) {
				char c = chars[offset];
				if(c <= 0x20) {
					if(start <= mark) {
						writtenMark = buffer.length() + mark - start; 
					}
					// skip this char and any other whitespace
					buffer.append(chars, start, offset - start);
					do {
						offset++;
						limit++;
					} while(chars[offset] <= 0x20);

					if(limit >= maxLimit) {
						limit = maxLimit;
					}

					start = offset;
					
					continue;
				}
				
				switch(c) {
				case '{' :
				case '[' :
					squareBrackets[level] = c == '[';

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
				case '"': {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;
					
					continue;
				}
				default : {
				}
				}
				offset++;
			}
			
			if(level == 0) {
				buffer.append(chars, start, offset - start);
			} else {
				int markLimit = MaxSizeJsonFilter.markToLimit(mark, chars[mark]);

				if(start < markLimit) {
					buffer.append(chars, start, markLimit - start);
				} else {
					buffer.setLength(MaxSizeJsonFilter.markToLimit(writtenMark, buffer.charAt(writtenMark)));
				}
				
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, buffer);
			}
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < maxLimit) {
					metrics.onMaxSize(maxLimit - mark - level);
				}
				
				metrics.onOutput(buffer.length() - bufferLength);
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

		int bufferLength = output.size();
		
		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);
		
		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

		try {
			int maxLimit = ByteWhitespaceFilter.skipWhitespaceBackwards(chars, length + offset);
			
			int start = offset;

			while(offset < limit) {
				
				byte c = chars[offset];
				if(c <= 0x20) {
					if(start <= mark) {
						writtenMark = stream.size() + mark - start; 
					}
					// skip this char and any other whitespace
					stream.write(chars, start, offset - start);
					do {
						offset++;
						limit++;
					} while(chars[offset] <= 0x20);

					if(limit >= maxLimit) {
						limit = maxLimit;
					}
				
					start = offset;

					continue;
				}
				
				switch(c) {
				case '{' :
				case '[' :
					squareBrackets[level] = c == '[';

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
				case '"': {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					offset++;

					continue;
				}
				default : {
				}
				}
				offset++;
			}

			if(level == 0) {
				stream.write(chars, start, offset - start);
				stream.writeTo(output);
			} else {
				int markLimit = MaxSizeJsonFilter.markToLimit(mark, chars[mark]);

				if(markLimit > start) {
					stream.write(chars, start, markLimit - start);
				} else {
					stream.setCount(MaxSizeJsonFilter.markToLimit(writtenMark, stream.getByte(writtenMark)));
				}
				
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, stream);

				stream.writeTo(output);
			}
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < maxLimit) {
					metrics.onMaxSize(maxLimit - mark - level);
				}
				
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
