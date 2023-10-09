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

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class SingleFullPathRemoveWhitespaceJsonFilter extends AbstractSingleCharArrayFullPathJsonFilter {

	protected SingleFullPathRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(type != FilterType.ANON && type != FilterType.PRUNE) {
			throw new IllegalArgumentException();
		}
	}

	public SingleFullPathRemoveWhitespaceJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathRemoveWhitespaceJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
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
		CharArrayWhitespaceFilter filter = new CharArrayWhitespaceFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		
		int bufferLength = buffer.length();
		int maxStringLength = this.maxStringLength;
		
		int level = 0;
		final char[][] elementPaths = this.pathChars;
		FilterType filterType = this.filterType;
		int pathMatches = this.maxPathMatches;

		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			int flushOffset = offset;

			while(offset < maxReadLimit) {
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

					buffer.append(chars, flushOffset, nextOffset - flushOffset);								

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
						offset = nextOffset;
						flushOffset = nextOffset;
						
						continue;
					}

					// was a field name
					
					buffer.append(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_CHARS && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setFlushOffset(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							flushOffset = filter.getFlushOffset();
						} else if(chars[nextOffset] == '[') {
							filter.setFlushOffset(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, buffer, metrics);
							
							flushOffset = filter.getFlushOffset();
						} else if(chars[nextOffset] == '"') {
							
							flushOffset = offset = nextOffset;
							
							nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, nextOffset);
							
							endQuoteIndex = nextOffset;
							
							buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);								
							
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
								
								buffer.append(filter.getPruneMessage());
								if(metrics != null) {
									metrics.onPrune(1);
								}

								flushOffset = offset;
							} else {
								filter.setFlushOffset(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, maxReadLimit, buffer, metrics);
								
								flushOffset = filter.getFlushOffset();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							if(filterType == FilterType.PRUNE) {
								buffer.append(filter.getPruneMessage());
								if(metrics != null) {
									metrics.onPrune(1);
								}

							} else {
								buffer.append(filter.getAnonymizeMessage());
								if(metrics != null) {
									metrics.onAnonymize(1);
								}
							}
							
							flushOffset = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								buffer.append(chars, flushOffset, offset - flushOffset);
								
								CharArrayWhitespaceFilter.process(chars, offset, maxReadLimit, buffer);
								
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

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		ByteArrayWhitespaceFilter filter = new ByteArrayWhitespaceFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
		
		int bufferLength = output.size();
		int maxStringLength = this.maxStringLength;
		
		int level = 0;
		final byte[][] elementPaths = this.pathBytes;
		
		FilterType filterType = this.filterType;
		int pathMatches = this.maxPathMatches;

		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);

			int flushOffset = offset;

			while(offset < maxReadLimit) {
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

					output.write(chars, flushOffset, nextOffset - flushOffset);								

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
						offset = nextOffset;
						flushOffset = nextOffset;
						
						continue;
					}
					
					// was a field name
					output.write(':');

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					if(elementPaths[level] != STAR_BYTES && !matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level])) {
						// skip here
						if(chars[nextOffset] == '{') {
							filter.setFlushOffset(nextOffset);
							
							offset = filter.skipObjectMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							flushOffset = filter.getFlushOffset();
						} else if(chars[nextOffset] == '[') {
							filter.setFlushOffset(nextOffset);
							
							offset = filter.skipArrayMaxStringLength(chars, nextOffset + 1, maxStringLength, output, metrics);
							
							flushOffset = filter.getFlushOffset();
						} else if(chars[nextOffset] == '"') {
							flushOffset = offset = nextOffset;
							
							nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, nextOffset);

							endQuoteIndex = nextOffset;
							
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);								
							
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

								output.write(filter.getPruneMessage());
								if(metrics != null) {
									metrics.onPrune(1);
								}
								
								flushOffset = offset;
							} else {
								filter.setFlushOffset(nextOffset);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, maxReadLimit, output, metrics);
								
								flushOffset = filter.getFlushOffset();
							}
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							if(filterType == FilterType.PRUNE) {
								output.write(filter.getPruneMessage());
								
								if(metrics != null) {
									metrics.onPrune(1);
								}
							} else {
								output.write(filter.getAnonymizeMessage());
								
								if(metrics != null) {
									metrics.onAnonymize(1);
								}
							}
							
							flushOffset = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								
								output.write(chars, flushOffset, offset - flushOffset);
								
								ByteArrayWhitespaceFilter.process(chars, offset, maxReadLimit, output);
								
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
