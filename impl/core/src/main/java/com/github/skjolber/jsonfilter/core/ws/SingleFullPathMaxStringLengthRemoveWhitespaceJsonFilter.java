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
			
			int start = offset;

			while(offset < limit) {
				char c = chars[offset];
				if(c <= 0x20) {
					// skip this char and any other whitespace
					buffer.append(chars, start, offset - start);
					do {
						offset++;
					} while(chars[offset] <= 0x20);

					start = offset;
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
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
					
					int endQuoteIndex = nextOffset;
					
					// key or value, might be whitespace
					nextOffset++;
					
					if(chars[nextOffset] != ':') {

						while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
							nextOffset++;
						}
						
						if(chars[nextOffset] != ':') {
							// was a value

							if(endQuoteIndex - offset >= maxStringLength) {
								addMaxLength(chars, offset, buffer, start, endQuoteIndex, metrics);
							} else {
								buffer.append(chars, start, endQuoteIndex - start + 1);			
							}
							
							offset = nextOffset;
							start = nextOffset;
							
							continue;
						}
					}

					buffer.append(chars, start, endQuoteIndex - start + 1);
					buffer.append(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_CHARS && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setStart(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							start = filter.getStart();
						} else if(chars[nextOffset] == '[') {
							filter.setStart(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							start = filter.getStart();
						} else if(chars[nextOffset] == '"') {
							
							start = offset = nextOffset;
							do {
								if(chars[nextOffset] == '\\') {
									nextOffset++;
								}
								nextOffset++;
							} while(chars[nextOffset] != '"');
							
							endQuoteIndex = nextOffset;
							
							if(endQuoteIndex - offset >= maxStringLength) {
								addMaxLength(chars, offset, buffer, start, endQuoteIndex, metrics);
							} else {
								buffer.append(chars, start, endQuoteIndex - start + 1);								
							}
							
							nextOffset++;

							offset = nextOffset;
							start = nextOffset;
						} else {
							start = nextOffset;
							offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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

								start = offset;
							} else {
								filter.setStart(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, buffer, metrics);
								
								start = filter.getStart();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							start = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// remove whitespace + max string length
								MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, limit, start, buffer, metrics, maxStringLength, truncateStringValue);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(buffer.length() - bufferLength);
								}
								
								return true;
							}							
						}
					} else {
						start = nextOffset;
						offset = nextOffset;
					}

					continue;
				}
				offset++;
			}
			buffer.append(chars, start, offset - start);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(buffer.length() - bufferLength);
			}			
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	private void addMaxLength(final char[] chars, int offset, final StringBuilder buffer, int start, int endQuoteIndex, JsonFilterMetrics metrics) {
		// was a value
		int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
		
		int removed = endQuoteIndex - aligned;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = removed - truncateStringValue.length - AbstractRangesFilter.lengthToDigits(removed);
		if(remove > 0) {
			buffer.append(chars, start, aligned - start);
			buffer.append(truncateStringValue);
			buffer.append(endQuoteIndex - aligned);
			buffer.append('"');
			
			if(metrics != null) {
				metrics.onMaxStringLength(1);
			}
		} else {
			buffer.append(chars, start, endQuoteIndex - start + 1);
		}
	}
	
	private void addMaxLength(final byte[] chars, int offset, final ByteArrayOutputStream output, int start, int endQuoteIndex, byte[] digit, JsonFilterMetrics metrics) {
		// was a value
		int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
		
		int removed = endQuoteIndex - aligned;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = removed - truncateStringValue.length - AbstractRangesFilter.lengthToDigits(removed);
		if(remove > 0) {
			output.write(chars, start, aligned - start);
			output.write(truncateStringValueAsBytes, 0, truncateStringValueAsBytes.length);
			ByteArrayRangesFilter.writeInt(output, removed, digit);
			output.write('"');
			
			if(metrics != null) {
				metrics.onMaxStringLength(1);
			}
		} else {
			output.write(chars, start, endQuoteIndex - start + 1);
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

			int start = offset;

			while(offset < limit) {
				byte c = chars[offset];
				if(c <= 0x20) {
					// skip this char and any other whitespace
					output.write(chars, start, offset - start);
					do {
						offset++;
					} while(chars[offset] <= 0x20);

					start = offset;
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
					int nextOffset = offset;
					do {
						if(chars[nextOffset] == '\\') {
							nextOffset++;
						}
						nextOffset++;
					} while(chars[nextOffset] != '"');
					
					int endQuoteIndex = nextOffset;
					
					// key or value, might be whitespace
					nextOffset++;
					
					if(chars[nextOffset] != ':') {

						while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
							nextOffset++;
						}
						
						if(chars[nextOffset] != ':') {
							// was a value
							if(endQuoteIndex - offset >= maxStringLength) {
								addMaxLength(chars, offset, output, start, endQuoteIndex, digit, metrics);
							} else {
								output.write(chars, start, endQuoteIndex - start + 1);								
							}

							offset = nextOffset;
							start = nextOffset;
							
							continue;
						}
					}

					output.write(chars, start, endQuoteIndex - start + 1);
					output.write(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_BYTES && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setStart(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							start = filter.getStart();
						} else if(chars[nextOffset] == '[') {
							filter.setStart(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							start = filter.getStart();
						} else if(chars[nextOffset] == '"') {
							start = offset = nextOffset;
							do {
								if(chars[nextOffset] == '\\') {
									nextOffset++;
								}
								nextOffset++;
							} while(chars[nextOffset] != '"');
							
							endQuoteIndex = nextOffset;
							
							if(endQuoteIndex - offset >= maxStringLength) {
								addMaxLength(chars, offset, output, start, endQuoteIndex, digit, metrics);
							} else {
								output.write(chars, start, endQuoteIndex - start + 1);								
							}
							
							nextOffset++;

							offset = nextOffset;
							start = nextOffset;							
						} else {
							start = nextOffset;
							offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
								
								start = offset;
							} else {
								filter.setStart(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, output, metrics);
								
								start = filter.getStart();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
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
							
							start = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// remove whitespace + max string length
								MaxStringLengthRemoveWhitespaceJsonFilter.processMaxStringLength(chars, offset, limit, start, output, filter.getDigit(), metrics, maxStringLength, truncateStringValueAsBytes);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(output.size() - bufferLength);
								}
								
								return true;
							}							
						}
					} else {
						start = nextOffset;
						offset = nextOffset;
					}

					continue;
				}
				offset++;
			}
			output.write(chars, start, offset - start);
			
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
