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

public class MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends MaxStringLengthRemoveWhitespaceJsonFilter {

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
			
			level = processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, buffer, level, squareBrackets, mark, writtenMark, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < maxReadLimit) {
					metrics.onMaxSize(maxReadLimit - mark - level);
				}
				
				metrics.onOutput(buffer.length() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	private int processMaxStringLengthMaxSize(final char[] chars, int offset, int maxSizeLimit, int maxReadLimit, final StringBuilder buffer, int level, boolean[] squareBrackets, int mark, int writtenMark, JsonFilterMetrics metrics) {
		
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
					super.processMaxStringLength(chars, offset, maxReadLimit, offset, buffer, metrics, maxStringLength, truncateStringValue);

					return 0;
				}

				writtenOffset = offset;
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
			
			case '"': {
				
				int nextOffset = offset;
				do {
					if(chars[nextOffset] == '\\') {
						nextOffset++;
					}
					nextOffset++;
				} while(chars[nextOffset] != '"');

				if(nextOffset - offset - 1 > maxStringLength) {
					nextOffset++;
					int postQuoteIndex = nextOffset;
					
					// key or value

					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}

					if(chars[nextOffset] == ':') {
						
						// was a key
						if(postQuoteIndex != nextOffset) {
							// did skip whitespace

							if(writtenOffset <= mark) {
								writtenMark = buffer.length() + mark - writtenOffset; 
							}
							buffer.append(chars, writtenOffset, postQuoteIndex - writtenOffset);
							
							maxSizeLimit += nextOffset - postQuoteIndex + 1;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							writtenOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} else {
						// was a value
						if(writtenOffset <= mark) {
							writtenMark = buffer.length() + mark - writtenOffset; 
						}
						int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						buffer.append(chars, writtenOffset, aligned - writtenOffset);
						buffer.append(truncateStringValue);
						buffer.append(postQuoteIndex - 1 - aligned);
						buffer.append('"');
						
						if(metrics != null) {
							metrics.onMaxStringLength(1);
						}
						
						maxSizeLimit += nextOffset - aligned; // also accounts for skipped whitespace, if any
						if(maxSizeLimit >= maxReadLimit) {
							super.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, buffer, metrics, maxStringLength, truncateStringValue);

							return 0;
						}
						
						writtenOffset = nextOffset;
					}
				} else {
					nextOffset++;
				}
				offset = nextOffset;

				continue;
			}
			default : {
			}
			}
			offset++;
		}

		if(level > 0) {
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			if(markLimit > writtenOffset) {
				buffer.append(chars, writtenOffset, markLimit - writtenOffset);
			} else {
				buffer.setLength(writtenMark);
			}
			MaxSizeJsonFilter.closeStructure(level, squareBrackets, buffer);
		} else {
			buffer.append(chars, writtenOffset, offset - writtenOffset);
		}		
		
		return level;
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}

		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);
		
		byte[] digit = new byte[11];
		
		int bufferLength = output.size();

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

			level = processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, stream, level, squareBrackets, mark, writtenMark, digit, metrics);
			stream.writeTo(output);
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(mark - level < maxReadLimit) {
					metrics.onMaxSize(maxReadLimit - mark - level);
				}
				
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}		
	}

	private int processMaxStringLengthMaxSize(byte[] chars, int offset, int maxSizeLimit, int maxReadLimit, FlexibleOutputStream stream, int level, boolean[] squareBrackets, int mark, int writtenMark, byte[] digit, JsonFilterMetrics metrics) throws IOException {
		
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
					super.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);
					
					return 0;
				}
			
				writtenOffset = offset;
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
			case '"': {
				
				int nextOffset = offset;
				do {
					if(chars[nextOffset] == '\\') {
						nextOffset++;
					}
					nextOffset++;
				} while(chars[nextOffset] != '"');
				
				if(nextOffset - offset - 1 > maxStringLength) {
					nextOffset++;
					int postQuoteIndex = nextOffset;
					
					// key or value

					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}					

					if(chars[nextOffset] == ':') {
						
						// was a key
						if(postQuoteIndex != nextOffset) {
							if(writtenOffset <= mark) {
								writtenMark = stream.size() + mark - writtenOffset; 
							}
							stream.write(chars, writtenOffset, postQuoteIndex - writtenOffset);
							
							maxSizeLimit += nextOffset - postQuoteIndex + 1;
							if(maxSizeLimit >= maxReadLimit) {
								super.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);
								return 0;
							}
							
							writtenOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} else {
						// was a value
						if(writtenOffset <= mark) {
							writtenMark = stream.size() + mark - writtenOffset; 
						}
						
						// was a value
						int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						stream.write(chars, writtenOffset, aligned - writtenOffset);
						stream.write(truncateStringValueAsBytes);
						ByteArrayRangesFilter.writeInt(stream, postQuoteIndex - 1 - aligned, digit);
						stream.write('"');
						
						if(metrics != null) {
							metrics.onMaxStringLength(1);
						}
						
						maxSizeLimit += nextOffset - aligned; // also account for skipped whitespace, if any
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						writtenOffset = nextOffset;
					}
				} else {
					nextOffset++;
				}
				offset = nextOffset;

				continue;
			}
			default : {
			}
			}
			offset++;
		}
		
		if(level > 0) {
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
			
			if(markLimit > writtenOffset) {
				stream.write(chars, writtenOffset, markLimit - writtenOffset);
			} else {
				stream.setCount(writtenMark);
			}
			MaxSizeJsonFilter.closeStructure(level, squareBrackets, stream);
		} else {
			stream.write(chars, writtenOffset, offset - writtenOffset);
		}
		
		return level;
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
