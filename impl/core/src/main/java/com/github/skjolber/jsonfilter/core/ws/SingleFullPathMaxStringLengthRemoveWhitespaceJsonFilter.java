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
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter extends AbstractSingleCharArrayFullPathJsonFilter {

	protected SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(type != FilterType.ANON && type != FilterType.PRUNE) {
			throw new IllegalArgumentException();
		}
	}

	public SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		return process(chars, offset, length, output, null);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		CharArrayWhitespaceFilter filter = new CharArrayWhitespaceFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		
		int bufferLength = buffer.length();
		
		int level = 0;
		final char[][] elementPaths = this.pathChars;
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		try {
			int limit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			int flushOffset = offset;

			while(offset < limit) {
				char c = chars[offset];
				if(c <= 0x20) {
					// skip this char and any other whitespace
					buffer.append(chars, flushOffset, offset - flushOffset);
					do {
						offset++;
					} while(chars[offset] <= 0x20);

					flushOffset = offset;
					c = chars[offset];
				}
				
				switch(c) {
				case '{' :
					level++;
					break;
				case '}' :
					level--;
					
					break;
				case '"' :					
					int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
					
					int endQuoteIndex = nextOffset;
					
					// key or value, might be whitespace
					nextOffset++;
					
					colon:
					if(chars[nextOffset] != ':') {

						if(chars[nextOffset] <= 0x20) {
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);

							if(chars[nextOffset] == ':') {
								break colon;
							}
						}
						// was a value

						if(endQuoteIndex - offset >= maxStringLength) {
							CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateStringValue, maxStringLength, metrics);
						} else {
							buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);			
						}
						
						offset = nextOffset;
						flushOffset = nextOffset;
						
						continue;
					}

					buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					buffer.append(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_CHARS && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setStart(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							flushOffset = filter.getStart();
						} else if(chars[nextOffset] == '[') {
							filter.setStart(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							flushOffset = filter.getStart();
						} else if(chars[nextOffset] == '"') {
							
							flushOffset = offset = nextOffset;
							
							nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
							
							endQuoteIndex = nextOffset;
							
							if(endQuoteIndex - offset >= maxStringLength) {
								CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateStringValue, maxStringLength, metrics);
							} else {
								buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);								
							}
							
							nextOffset++;

							offset = nextOffset;
							flushOffset = nextOffset;
						} else {
							flushOffset = nextOffset;
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
						continue;
					}

					if(level + 1 == elementPaths.length) {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								// skip both whitespace and actual content
								offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
								
								buffer.append(pruneJsonValue);
								if(metrics != null) {
									metrics.onPrune(1);
								}

								flushOffset = offset;
							} else {
								filter.setStart(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, buffer, metrics);
								
								flushOffset = filter.getStart();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							if(filterType == FilterType.PRUNE) {
								buffer.append(pruneJsonValue);
								if(metrics != null) {
									metrics.onPrune(1);
								}

							} else {
								buffer.append(anonymizeJsonValue);
								if(metrics != null) {
									metrics.onAnonymize(1);
								}
							}
							
							flushOffset = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// remove whitespace + max string length
								MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, limit, flushOffset, buffer, metrics, maxStringLength, truncateStringValue);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(buffer.length() - bufferLength);
								}
								
								return true;
							}							
						}
					} else {
						flushOffset = nextOffset;
						offset = nextOffset;
					}

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
		ByteArrayWhitespaceFilter filter = new ByteArrayWhitespaceFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
		
		int bufferLength = output.size();
		
		int level = 0;
		final byte[][] elementPaths = this.pathBytes;
		
		byte[] digit = new byte[11];
		
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		try {
			int limit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);

			int flushOffset = offset;

			while(offset < limit) {
				byte c = chars[offset];
				if(c <= 0x20) {
					// skip this char and any other whitespace
					output.write(chars, flushOffset, offset - flushOffset);
					do {
						offset++;
					} while(chars[offset] <= 0x20);

					flushOffset = offset;
					c = chars[offset];
				}
				
				switch(c) {
				case '{' :
					level++;
					break;
				case '}' :
					level--;
					
					break;
				case '"' :					
					int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
					
					int endQuoteIndex = nextOffset;
					
					// key or value, might be whitespace
					nextOffset++;
					
					colon:
					if(chars[nextOffset] != ':') {

						if(chars[nextOffset] <= 0x20) {
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);

							if(chars[nextOffset] == ':') {
								break colon;
							}
						}
							
						// was a value
						if(endQuoteIndex - offset >= maxStringLength) {
							ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateStringValueAsBytes, maxStringLength, digit, metrics);
						} else {
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);								
						}
						
						offset = nextOffset;
						flushOffset = nextOffset;
						
						continue;
					}
					
					output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					output.write(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_BYTES && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setStart(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							flushOffset = filter.getStart();
						} else if(chars[nextOffset] == '[') {
							filter.setStart(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							flushOffset = filter.getStart();
						} else if(chars[nextOffset] == '"') {
							flushOffset = offset = nextOffset;
							
							nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
							
							endQuoteIndex = nextOffset;
							
							if(endQuoteIndex - offset >= maxStringLength) {
								ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateStringValueAsBytes, maxStringLength, digit, metrics);
							} else {
								output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);								
							}
							
							nextOffset++;

							offset = nextOffset;
							flushOffset = nextOffset;							
						} else {
							flushOffset = nextOffset;
							offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
						continue;
					}

					if(level + 1 == elementPaths.length) {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								// skip both whitespace and actual content
								offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);

								output.write(pruneJsonValueAsBytes);
								if(metrics != null) {
									metrics.onPrune(1);
								}
								
								flushOffset = offset;
							} else {
								filter.setStart(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, output, metrics);
								
								flushOffset = filter.getStart();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							if(filterType == FilterType.PRUNE) {
								output.write(pruneJsonValueAsBytes);
								
								if(metrics != null) {
									metrics.onPrune(1);
								}

							} else {
								output.write(anonymizeJsonValueAsBytes);
								
								if(metrics != null) {
									metrics.onAnonymize(1);
								}

							}
							
							flushOffset = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// remove whitespace + max string length
								MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, limit, flushOffset, output, filter.getDigit(), metrics, maxStringLength, truncateStringValueAsBytes);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(output.size() - bufferLength);
								}
								
								return true;
							}							
						}
					} else {
						flushOffset = nextOffset;
						offset = nextOffset;
					}

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
	public boolean isRemovingWhitespace() {
		return true;
	}

}