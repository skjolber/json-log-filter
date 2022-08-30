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

import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;

public class MaxSizePrettyPrintJsonFilter extends PrettyPrintJsonFilter {

	public MaxSizePrettyPrintJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizePrettyPrintJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MaxSizePrettyPrintJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, buffer);
		}

		length += offset;

		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		try {
			int start = offset;

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
				case '"': {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					if(nextOffset - offset - 1 > maxStringLength) {
						nextOffset++;

						// key or value, might be whitespace
						int end = nextOffset;

						// skip whitespace
						// optimization: scan for highest value
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(chars[nextOffset] == ':') {
							// was a key
							buffer.append(chars, start, end - start);

							// removed whitespace, increment limit correspondingly
							limit += nextOffset - end;
						} else {
							// was a value
							int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							buffer.append(chars, start, aligned - start);
							buffer.append(truncateStringValue);
							buffer.append(end - aligned - 1);
							buffer.append('"');

							limit += nextOffset - aligned - truncateStringValue.length;
						}

						start = nextOffset;
					}
					offset = nextOffset + 1;

					continue;
				}
				default : {
					if(chars[offset] <= 0x20) {
						// skip this char and any other whitespace
						buffer.append(chars, start, offset - start);
						do {
							offset++;
							limit++;
						} while(chars[offset] <= 0x20);

						start = offset;

						continue;
					}
				}
				}
				offset++;
			}
			mark = MaxSizeJsonFilter.alignMark(mark, chars);

			buffer.append(chars, start, mark);

			MaxSizeJsonFilter.closeStructure(level, squareBrackets, buffer);

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output);
		}

		length += offset;

		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;

		byte[] digit = new byte[11];

		try {
			int start = offset;

			loop:
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
				case '"': {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					if(nextOffset - offset - 1 > maxStringLength) {
						nextOffset++;

						// key or value, might be whitespace
						int end = nextOffset;

						// skip whitespace
						// optimization: scan for highest value
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(chars[nextOffset] == ':') {
							
							if(offset + end - start > limit) {
								// too much 
								break loop;
							}
							
							// was a key
							output.write(chars, start, end - start);

							// removed whitespace, increment limit correspondingly
							limit += nextOffset - end;
						} else {
							// was a value
							int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							
							
							if(offset + aligned - start > limit) {
								// too much 
								break loop;
							}
							
							output.write(chars, start, aligned - start);
							output.write(truncateStringValueAsBytes);
							ByteArrayRangesFilter.writeInt(output, end - aligned - 1, digit);
							output.write('"');

							// removed whitespace + part of string, increment limit correspondingly
							limit += nextOffset - aligned - truncateStringValueAsBytes.length;
						}

						start = nextOffset;
					}
					offset = nextOffset + 1;

					continue;
				}
				default : {
					if(chars[offset] <= 0x20) {
						// skip this char and any other whitespace
						output.write(chars, start, offset - start);
						do {
							offset++;
							limit++;
						} while(chars[offset] <= 0x20);

						start = offset;

						continue;
					}
				}
				}
				offset++;
			}

			mark = MaxSizeJsonFilter.alignMark(mark, chars);

			output.write(chars, start, mark);

			MaxSizeJsonFilter.closeStructure(level, squareBrackets, output);

			return true;
		} catch(Exception e) {
			return false;
		}

	}
}
