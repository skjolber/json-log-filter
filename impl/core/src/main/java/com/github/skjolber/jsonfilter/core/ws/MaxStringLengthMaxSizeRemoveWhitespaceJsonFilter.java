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

import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
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
			
			level = processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, buffer, level, squareBrackets, mark, writtenMark, maxStringLength, truncateStringValue, metrics);
			
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

	public static int processMaxStringLengthMaxSize(final char[] chars, int offset, int maxSizeLimit, int maxReadLimit, final StringBuilder buffer, int bracketLevel, boolean[] squareBrackets, int mark, int streamMark, int maxStringLength, char[] truncateStringValue, JsonFilterMetrics metrics) {
		
		int flushedOffset = offset;

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
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, buffer, metrics, maxStringLength, truncateStringValue);

					return 0;
				}

				flushedOffset = offset;
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
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, buffer, metrics, maxStringLength, truncateStringValue);

					return 0;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case ',' :
				mark = offset;
				break;			
			
			case '"': {
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;
				
				nextOffset++;

				if(endQuoteIndex - offset < maxStringLength) {
					offset = nextOffset;

					continue;
				}

				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);
						
						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						if(maxSizeLimit >= maxReadLimit) {
							MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, buffer, metrics, maxStringLength, truncateStringValue);

							return 0;
						}

						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					
					if(flushedOffset <= mark) {
						streamMark = buffer.length() + mark - flushedOffset; 
					}
					
					// was a value
					maxSizeLimit += CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushedOffset, endQuoteIndex, truncateStringValue, maxStringLength, metrics);
					if(maxSizeLimit >= maxReadLimit) {
						MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, buffer, metrics, maxStringLength, truncateStringValue);

						return 0;
					}
					
					offset = nextOffset;
					flushedOffset = nextOffset;

					continue;
				}

				// was a key
				if(flushedOffset <= mark) {
					streamMark = buffer.length() + mark - flushedOffset; 
				}
				buffer.append(chars, flushedOffset, endQuoteIndex - flushedOffset + 1);
				buffer.append(':');

				nextOffset++; 
				
				offset = nextOffset;
				
				if(chars[nextOffset] <= 0x20) {
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					maxSizeLimit += nextOffset - endQuoteIndex - 1;
					if(maxSizeLimit >= maxReadLimit) {
						MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, buffer, metrics, maxStringLength, truncateStringValue);

						return 0;
					}
				}
					
				if(maxSizeLimit >= maxReadLimit) {
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, nextOffset, maxReadLimit, nextOffset, buffer, metrics, maxStringLength, truncateStringValue);

					return 0;
				}
				
				flushedOffset = nextOffset;
				offset = nextOffset;

				continue;

			}
			default : {
			}
			}
			offset++;
		}

		if(bracketLevel > 0) {
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						buffer.append(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				} else {
					if(mark >= flushedOffset) {
						streamMark = buffer.length() + mark - flushedOffset; 
						
						buffer.append(chars, flushedOffset, mark - flushedOffset);
					}					
				}
				buffer.setLength(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		} else {
			buffer.append(chars, flushedOffset, offset - flushedOffset);
		}		
		
		return bracketLevel;
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}
		
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

			level = processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, output, level, squareBrackets, mark, writtenMark, digit, maxStringLength, truncateStringValueAsBytes, metrics);
			
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

	public static int processMaxStringLengthMaxSize(byte[] chars, int offset, int maxSizeLimit, int maxReadLimit, ResizableByteArrayOutputStream stream, int bracketLevel, boolean[] squareBrackets, int mark, int streamMark, byte[] digit, int maxStringLength, byte[] truncateStringValueAsBytes, JsonFilterMetrics metrics) throws IOException {
		
		int flushedOffset = offset;

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
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);
					
					return 0;
				}
			
				flushedOffset = offset;
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
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);
					
					return 0;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case ',' :
				mark = offset;
				break;			
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;
				
				nextOffset++;

				if(endQuoteIndex - offset < maxStringLength) {
					offset = nextOffset;

					continue;
				}

				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);
						
						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						if(maxSizeLimit >= maxReadLimit) {
							MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);

							return 0;
						}

						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					
					if(flushedOffset <= mark) {
						streamMark = stream.size() + mark - flushedOffset; 
					}
					
					// was a value
					maxSizeLimit += ByteArrayWhitespaceFilter.addMaxLength(chars, offset, stream, flushedOffset, endQuoteIndex, truncateStringValueAsBytes, maxStringLength, digit, metrics);
					if(maxSizeLimit >= maxReadLimit) {
						MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);

						return 0;
					}
					
					offset = nextOffset;
					flushedOffset = nextOffset;

					continue;
				}

				// was a key
				if(flushedOffset <= mark) {
					streamMark = stream.size() + mark - flushedOffset; 
				}
				stream.write(chars, flushedOffset, endQuoteIndex - flushedOffset + 1);
				stream.write(':');

				nextOffset++; 
				
				offset = nextOffset;
				
				if(chars[nextOffset] <= 0x20) {
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					maxSizeLimit += nextOffset - endQuoteIndex - 1;
					if(maxSizeLimit >= maxReadLimit) {
						MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);

						return 0;
					}
				}
					
				if(maxSizeLimit >= maxReadLimit) {
					MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, maxReadLimit, offset, stream, digit, metrics, maxStringLength, truncateStringValueAsBytes);

					return 0;
				}
				
				flushedOffset = nextOffset;
				offset = nextOffset;

				continue;				
			}
			default : {
			}
			}
			offset++;
		}
		
		if(bracketLevel > 0) {
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						stream.write(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				} else {
					if(mark >= flushedOffset) {
						streamMark = stream.size() + mark - flushedOffset; 
						
						stream.write(chars, flushedOffset, mark - flushedOffset);
					}					
				}
				stream.setSize(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, stream);
		} else {
			stream.write(chars, flushedOffset, offset - flushedOffset);
		}
		
		return bracketLevel;
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
