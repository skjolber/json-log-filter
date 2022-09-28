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
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.FlexibleOutputStream;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;

public class MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends RemoveWhitespaceJsonFilter {

	public MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize) {
		this(maxStringLength, maxSize, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, buffer, metrics);
		}
		
		int bufferLength = buffer.length();
		int lengthLimit = length + offset;

		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

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
						int endQuoteIndex = nextOffset;

						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							
							// was a key
							if(endQuoteIndex != nextOffset) {
								// did skip whitespace

								if(start <= mark) {
									writtenMark = buffer.length() + mark - start; 
								}
								buffer.append(chars, start, endQuoteIndex - start + 1);
								
								limit += nextOffset - endQuoteIndex;
								if(limit >= lengthLimit) {
									limit = lengthLimit;
								}
								
								start = nextOffset;
								offset = nextOffset;
								continue;
							}
						} else {
							// was a value
							if(start <= mark) {
								writtenMark = buffer.length() + mark - start; 
							}
							int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							buffer.append(chars, start, aligned - start);
							buffer.append(truncateStringValue);
							buffer.append(endQuoteIndex - aligned);
							buffer.append('"');
							
							if(metrics != null) {
								metrics.onMaxStringLength(1);
							}
							
							limit += nextOffset - aligned; // also account for skipped whitespace, if any
							if(limit >= length) {
								limit = length;
							}
							
							start = nextOffset;
						}
					} else {
						nextOffset++;
					}
					offset = nextOffset;

					continue;
				}
				default : {
					if(chars[offset] <= 0x20) {
						// skip this char and any other whitespace
						if(start <= mark) {
							writtenMark = buffer.length() + mark - start; 
						}
						buffer.append(chars, start, offset - start);
						do {
							offset++;
							limit++;
						} while(offset < lengthLimit && chars[offset] <= 0x20);

						if(limit >= lengthLimit) {
							limit = lengthLimit;
						}
						start = offset;

						continue;
					}
				}
				}
				offset++;
			}
			
			if(level == 0) {
				buffer.append(chars, start, offset - start);
			} else {
				int deltaMark = deltaMark(mark, chars[mark]);

				if(mark + deltaMark > start) {
					buffer.append(chars, start, mark - start + deltaMark);
				} else {
					buffer.setLength(writtenMark + deltaMark(writtenMark, buffer.charAt(writtenMark)));
				}
				
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, buffer);
			}
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < lengthLimit) {
					metrics.onMaxSize(lengthLimit - mark - level);
				}
				
				metrics.onOutput(buffer.length() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}

		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);
		
		byte[] digit = new byte[11];
		
		int bufferLength = output.size();
		int lengthLimit = length + offset;

		int limit = offset + maxSize;

		int level = 0;

		boolean[] squareBrackets = new boolean[32];

		int mark = 0;
		int writtenMark = 0;

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
						int endQuoteIndex = nextOffset;

						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							
							// was a key
							if(endQuoteIndex != nextOffset) {
								// did skip whitespace
								// did skip whitespace

								if(start <= mark) {
									writtenMark = stream.size() + mark - start; 
								}
								stream.write(chars, start, endQuoteIndex - start + 1);
								
								limit += nextOffset - endQuoteIndex;
								if(limit >= lengthLimit) {
									limit = lengthLimit;
								}
								
								start = nextOffset;
								offset = nextOffset;
								continue;
							}
						} else {
							// was a value
							if(start <= mark) {
								writtenMark = stream.size() + mark - start; 
							}
							
							// was a value
							int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							stream.write(chars, start, aligned - start);
							stream.write(truncateStringValueAsBytes);
							ByteArrayRangesFilter.writeInt(stream, endQuoteIndex - aligned, digit);
							stream.write('"');
							
							if(metrics != null) {
								metrics.onMaxStringLength(1);
							}
							
							limit += nextOffset - aligned; // also account for skipped whitespace, if any
							if(limit >= length) {
								limit = length;
							}
							
							start = nextOffset;
						}
					} else {
						nextOffset++;
					}
					offset = nextOffset;

					continue;
				}
				default : {
					if(chars[offset] <= 0x20) {
						// skip this char and any other whitespace
						if(start <= mark) {
							writtenMark = stream.size() + mark - start; 
						}
						stream.write(chars, start, offset - start);
						do {
							offset++;
							limit++;
						} while(offset < lengthLimit && chars[offset] <= 0x20);

						if(limit >= lengthLimit) {
							limit = lengthLimit;
						}
						start = offset;

						continue;
					}
				}
				}
				offset++;
			}
			
			if(level == 0) {
				stream.write(chars, start, offset - start);
				stream.writeTo(output);
			} else {
				int deltaMark = deltaMark(mark, chars[mark]);

				if(mark + deltaMark > start) {
					stream.write(chars, start, mark - start + deltaMark);
				} else {
					stream.setCount(writtenMark + deltaMark(writtenMark, stream.getByte(writtenMark)));
				}
				
				MaxSizeJsonFilter.closeStructure(level, squareBrackets, stream);

				stream.writeTo(output);
			}
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < lengthLimit) {
					metrics.onMaxSize(lengthLimit - mark - level);
				}
				
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}		
	}
	
	public static int deltaMark(int mark, char c) {
		switch(c) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return 1;
			default : {
				return 0;
			}
		}
	}
	
	public static int deltaMark(int mark, byte c) {
		switch(c) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return 1;
			default : {
				return 0;
			}
		}
	}
}