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
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

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
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int bufferLength = buffer.length();
		
		int limit = length + offset;
		
		int level = 0;
		final char[][] elementPaths = this.pathChars;
		int matches = 0;
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		try {
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

					continue;
				}
				
				switch(chars[offset]) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						offset = CharArrayRangesFilter.skipObject(chars, offset);
						level--;
						
						continue;
					}
					break;
				case '}' :
					level--;
					
					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '"' :					
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					int endQuoteIndex = nextOffset;
						
					// key or value, might be whitespace

					// skip whitespace
					// optimization: scan for highest value
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] == ':') {
						// was a key
						buffer.append(chars, start, endQuoteIndex - start + 1);
						
						if(matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset;
							
							start = nextOffset;
							
							continue;
						}
						
						if(matches == elementPaths.length) {
							if(filterType == FilterType.PRUNE) {
								
								
								//filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipSubtree(chars, nextOffset));
							} else {
								if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
									// filter as tree
									//offset = CharArrayRangesFilter.anonymizeSubtree(chars, nextOffset, filter);
								} else {
									if(chars[nextOffset] == '"') {
										// quoted value
										offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
									} else {

										offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
									}
									//filter.addAnon(nextOffset, offset);
								}
							}
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									// just remove whitespace
								}							
							}
							
							matches--;
						} else {
							offset = nextOffset;
						}
						
						continue;
						
						

					} else {

					}

					offset = nextOffset + 1;

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

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int limit = length + offset;
		
		int bufferLength = output.size();

		byte[] digit = new byte[11];

		try {
			int start = offset;

			while(offset < limit) {
				byte c = chars[offset];
				if(c == '"') {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					if(nextOffset - offset - 1 > maxStringLength) {
						int endQuoteIndex = nextOffset;
						
						// key or value, might be whitespace

						// skip whitespace
						// optimization: scan for highest value
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// was a key
							output.write(chars, start, endQuoteIndex - start + 1);
							
							
							
						} else {
							// was a value
						}

						start = nextOffset;
					}
					offset = nextOffset + 1;

					continue;
				} else if(c <= 0x20) {
					// skip this char and any other whitespace
					output.write(chars, start, offset - start);
					do {
						offset++;
					} while(chars[offset] <= 0x20);

					start = offset;

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

	public boolean anonymizeSubtree(final char[] chars, int offset, int limit, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int start = offset;

		while(offset < limit) {
			char c = chars[offset];
			if(c == '"') {
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

				if(nextOffset - offset - 1 > maxStringLength) {
					int endQuoteIndex = nextOffset;
					
					// key or value, might be whitespace

					// skip whitespace
					// optimization: scan for highest value
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] == ':') {
						// was a key
						buffer.append(chars, start, endQuoteIndex - start + 1);
					} else {
						// was a value
						int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						buffer.append(chars, start, aligned - start);
						buffer.append(truncateStringValue);
						buffer.append(endQuoteIndex - aligned);
						buffer.append('"');
						
						if(metrics != null) {
							metrics.onMaxStringLength(1);
						}
					}

					start = nextOffset;
				}
				offset = nextOffset + 1;

				continue;
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				start = offset;

				continue;
			}
			offset++;
		}
		buffer.append(chars, start, offset - start);
			
			
	}

}
