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
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class MaxStringLengthRemoveWhitespaceJsonFilter extends AbstractJsonFilter {

	public MaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		return process(chars, offset, length, output, null);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int bufferLength = buffer.length();
		
		try {
			int limit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);

			processMaxStringLength(chars, offset, limit, offset, buffer, metrics, maxStringLength, truncateStringValue);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(buffer.length() - bufferLength);
			}			
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public static void processMaxStringLength(final char[] chars, int offset, int limit, int flushOffset, final StringBuilder buffer, JsonFilterMetrics metrics, int maxStringLength, char[] truncateMessage) {
		while(offset < limit) {
			char c = chars[offset];
			if(c == '"') {
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int endQuoteIndex = nextOffset;
				
				// key or value, might be followed by whitespace
				nextOffset++;
				
				if(endQuoteIndex - offset < maxStringLength) {
					offset = nextOffset;

					continue;
				}
				
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// whitespace before colon
							buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
							buffer.append(':');
							
							nextOffset++;

							if(chars[nextOffset] <= 0x20) {
								// whitespace before and after colon
								do {
									nextOffset++;
								} while(chars[nextOffset] <= 0x20);				
							} else {
								// whitespace before colon, but not after
							}

							flushOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} 
					
					// was a value
					if(endQuoteIndex - offset >= maxStringLength) {
						CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, metrics);
						
						flushOffset = nextOffset;
					}
						
					offset = nextOffset;
					
					continue;
				} else {
					// was a key
					nextOffset++;

					if(chars[nextOffset] > 0x20) {
						// no whitespace before or after colon
						
						offset = nextOffset;
						continue;
					}

					// whitespace after colon, but not before
					buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					buffer.append(':');
					
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);				
					
					flushOffset = nextOffset;
					offset = nextOffset;
				}
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
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int bufferLength = output.size();

		byte[] digit = new byte[11];

		try {
			int limit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			processMaxStringLength(chars, offset, limit, offset, output, digit, metrics, maxStringLength, truncateStringValueAsBytes);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(output.size() - bufferLength);
			}	
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public static void processMaxStringLength(byte[] chars, int offset, int limit, int flushOffset, ResizableByteArrayOutputStream output, byte[] digit, JsonFilterMetrics metrics, int maxStringLength, byte[] truncateMessage) throws IOException {
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int endQuoteIndex = nextOffset;
				
				// key or value, might be followed by whitespace
				nextOffset++;
				
				if(endQuoteIndex - offset < maxStringLength) {
					offset = nextOffset;

					continue;
				}
				
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// whitespace before colon
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
							output.write(':');
							
							nextOffset++;

							if(chars[nextOffset] <= 0x20) {
								// whitespace before and after colon
								do {
									nextOffset++;
								} while(chars[nextOffset] <= 0x20);				
							} else {
								// whitespace before colon, but not after
							}

							flushOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} 
					
					// was a value
					if(endQuoteIndex - offset >= maxStringLength) {
						ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, digit, metrics);
						
						flushOffset = nextOffset;
					}
						
					offset = nextOffset;
					
					continue;
				} else {
					// was a key
					nextOffset++;

					if(chars[nextOffset] > 0x20) {
						// no whitespace before or after colon
						
						offset = nextOffset;
						continue;
					}

					// whitespace after colon, but not before
					output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					output.write(':');
					
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);				
					
					flushOffset = nextOffset;
					offset = nextOffset;
				}
				continue;
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20 );					

				flushOffset = offset;

				continue;
			}
			offset++;
		}
		output.write(chars, flushOffset, offset - flushOffset);
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
