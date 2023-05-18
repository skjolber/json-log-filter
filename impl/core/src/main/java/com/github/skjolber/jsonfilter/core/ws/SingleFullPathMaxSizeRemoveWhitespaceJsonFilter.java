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
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class SingleFullPathMaxSizeRemoveWhitespaceJsonFilter extends SingleFullPathRemoveWhitespaceJsonFilter {

	protected SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
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
		CharArrayWhitespaceBracketFilter filter = new CharArrayWhitespaceBracketFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		
		int bufferLength = buffer.length();
		
		int maxSizeLimit = offset + maxSize;
		
		int level = 0;
		final char[][] elementPaths = this.pathChars;
		int matches = 0;
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		int bracketLevel = filter.getLevel();

		boolean[] squareBrackets = filter.getSquareBrackets();
		
		int mark = 0;
		int writtenMark = 0;

		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			
			int start = offset;

			while(offset < maxSizeLimit) {
				char c = chars[offset];
				
				if(c <= 0x20) {
					if(start <= mark) {
						writtenMark = buffer.length() + mark - start; 
					}
					// skip this char and any other whitespace
					buffer.append(chars, start, offset - start);
					do {
						offset++;
						maxSizeLimit++;
					} while(chars[offset] <= 0x20);

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}

					start = offset;
					c = chars[offset];
				}
				switch(c) {
				case '{' :
					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					if(level > matches) {
						// so always level < elementPaths.length
						
						filter.setLimit(maxSizeLimit);
						filter.setStart(start);
						filter.setLevel(bracketLevel);
						filter.setMark(mark);
						filter.setWrittenMark(writtenMark);
						
						offset = filter.skipObjectOrArrayMaxSize(chars, offset + 1, maxReadLimit, buffer);
						
						start = filter.getStart();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						writtenMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();

						continue;
					}
					mark = offset;

					level++;
					break;
				case '}' :
					level--;
					bracketLevel--;

					mark = offset;

					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '[' : {
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					mark = offset;

					break;
				}
				case ']' :
					bracketLevel--;
					
					mark = offset;

					break;
				case ',' :
					mark = offset;
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

					buffer.append(chars, start, endQuoteIndex - start + 1);
					
					if(chars[nextOffset] != ':') {
						// was a value
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					// reset match for a sibling field name, if any
					matches = level - 1;

					// was a field name
					if(elementPaths[matches] == STAR_CHARS || matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset + 1;
						start = nextOffset;
						
						continue;
					}
					
					if(matches == elementPaths.length) {
						buffer.append(':');
						
						// skip whitespace
						// optimization: scan for highest value
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

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
								filter.setLimit(maxSizeLimit);
								filter.setStart(nextOffset);
								filter.setLevel(bracketLevel);
								filter.setMark(mark);
								filter.setWrittenMark(writtenMark);
								
								offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset, maxReadLimit, buffer, metrics);
								
								System.out.println(offset + " / " + chars.length + " " + chars[offset]);

								start = filter.getStart();
								bracketLevel = filter.getLevel();
								mark = filter.getMark();
								writtenMark = filter.getWrittenMark();
								squareBrackets = filter.getSquareBrackets();
								maxSizeLimit = filter.getLimit();
								
								System.out.println("Limit is " + maxSizeLimit);
								
								System.out.println("Max limit is " + maxReadLimit);
								

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
								// just remove whitespace
								CharArrayWhitespaceFilter.process(chars, nextOffset, maxSizeLimit, buffer);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(buffer.length() - bufferLength);
								}
								
								return true;
							}							
						}
						
						matches--;
					} else {
						start = nextOffset;
						offset = nextOffset;
					}

					continue;
				}
				offset++;
			}

			System.out.println(level);
			System.out.println(writtenMark);
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
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		ByteArrayWhitespaceBracketFilter filter = new ByteArrayWhitespaceBracketFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);

		int bufferLength = output.size();
		
		int maxSizeLimit = offset + maxSize;
		
		int level = 0;
		final byte[][] elementPaths = this.pathBytes;
		int matches = 0;
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		int bracketLevel = filter.getLevel();

		boolean[] squareBrackets = filter.getSquareBrackets();
		
		int mark = 0;
		int writtenMark = 0;

		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			
			int start = offset;

			while(offset < maxSizeLimit) {
				byte c = chars[offset];
				if(c <= 0x20) {
					if(start <= mark) {
						writtenMark = output.size() + mark - start; 
					}
					// skip this char and any other whitespace
					output.write(chars, start, offset - start);
					do {
						offset++;
						maxSizeLimit++;
					} while(chars[offset] <= 0x20);

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}

					start = offset;
					c = chars[offset];
				}
				
				switch(c) {
				case '{' :
					squareBrackets[bracketLevel] = false;
					bracketLevel++;
					
					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}

					if(level > matches) {
						// so always level < elementPaths.length
						
						filter.setLimit(maxSizeLimit);
						filter.setStart(start);
						filter.setLevel(bracketLevel);
						filter.setMark(mark);
						filter.setWrittenMark(writtenMark);
						
						offset = filter.skipObjectOrArrayMaxSize(chars, offset, maxReadLimit, output);
						
						start = filter.getStart();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						writtenMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();
						
						System.out.println(offset + " / " + chars.length);
						
						continue;
					}
					mark = offset;

					level++;
					break;
				case '}' :
					level--;
					bracketLevel--;

					mark = offset;

					// always skips start object if not on a matching level, so must always constrain here
					matches = level;
					
					break;
				case '[' : {
					squareBrackets[bracketLevel] = true;
					bracketLevel++;

					if(bracketLevel >= squareBrackets.length) {
						squareBrackets = filter.grow(squareBrackets);
					}
					mark = offset;

					break;
				}
				case ']' :
					bracketLevel--;
					
					mark = offset;

					break;
				case ',' :
					mark = offset;
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

					output.write(chars, start, endQuoteIndex - start + 1);
					
					if(chars[nextOffset] != ':') {
						// was a value
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					// reset match for a sibling field name, if any
					matches = level - 1;

					// was a field name
					if(elementPaths[matches] == STAR_BYTES || matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset + 1;
						start = nextOffset;
						
						continue;
					}
					
					if(matches == elementPaths.length) {
						output.write(':');
						
						// skip whitespace
						// optimization: scan for highest value
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

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
								filter.setLevel(bracketLevel);
								filter.setMark(mark);
								filter.setWrittenMark(writtenMark);
								filter.setLimit(maxSizeLimit);

								offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, output, metrics);
								
								start = filter.getStart();
								bracketLevel = filter.getLevel();
								mark = filter.getMark();
								writtenMark = filter.getWrittenMark();
								squareBrackets = filter.getSquareBrackets();
								maxSizeLimit = filter.getLimit();
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
								// just remove whitespace
								ByteArrayWhitespaceFilter.process(chars, nextOffset, maxSizeLimit, output);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(output.size() - bufferLength);
								}
								
								return true;
							}							
						}
						
						matches--;
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
