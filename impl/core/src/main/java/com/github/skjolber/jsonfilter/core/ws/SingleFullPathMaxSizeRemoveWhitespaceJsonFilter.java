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
import com.github.skjolber.jsonfilter.base.AbstractSingleCharArrayFullPathJsonFilter;
import com.github.skjolber.jsonfilter.base.FlexibleOutputStream;
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
		this(-1, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleFullPathMaxSizeRemoveWhitespaceJsonFilter(int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxSize, maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, buffer, metrics);
		}

		int bufferLength = buffer.length();

		int maxSizeLimit = offset + maxSize;
		
		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}

			CharArrayWhitespaceBracketFilter filter = new CharArrayWhitespaceBracketFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);

			filter.setLimit(maxSizeLimit);
			
			int level = processMaxSize(chars, offset, maxReadLimit, 0, buffer, this.pathChars, 0, filterType, maxPathMatches, filter, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				
				if(level > 0) {
					metrics.onMaxSize(-1);
				}
				
				metrics.onOutput(buffer.length() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected int processMaxSize(final char[] chars, int offset, int maxReadLimit, int level, final StringBuilder buffer, final char[][] elementPaths, int matches, FilterType filterType, int pathMatches, CharArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) {
		int maxSizeLimit = filter.getLimit();

		int start = filter.getStart();
		int mark = filter.getMark();
		int writtenMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();

		loop:
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

					filter.setLevel(bracketLevel);
					filter.setMark(mark);

					filter.setLimit(maxSizeLimit);
					filter.setStart(start);
					filter.setWrittenMark(writtenMark);
					
					offset = filter.skipObjectOrArrayMaxSize(chars, offset + 1, maxReadLimit, buffer);

					bracketLevel = filter.getLevel();
					mark = filter.getMark();

					start = filter.getStart();
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
					
					if(filterType == FilterType.PRUNE) {

						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}
						}
						
						buffer.append(filter.getPruneMessage());
						if(metrics != null) {
							metrics.onPrune(1);
						}
						
						// adjust max size limit
						maxSizeLimit += offset - nextOffset - pruneJsonValue.length;
						
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
							
							// TODO dump the rest of the document
						}
						
						if(offset >= maxSizeLimit) {
							// make the previously flushed 'written mark' count
							start = offset;
							
							// TODO this might in some cases exclude the last field + value in a document 
							// which ends with a lot of whitespace but this is acceptable
							
							break loop;
						} else {
							mark = offset;
							start = offset;
						}
					} else {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							
							filter.setLimit(maxSizeLimit);
							filter.setStart(nextOffset);
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							filter.setWrittenMark(writtenMark);
							
							offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset, maxReadLimit, buffer, metrics);
							
							start = filter.getStart();
							bracketLevel = filter.getLevel();
							mark = filter.getMark();
							writtenMark = filter.getWrittenMark();
							squareBrackets = filter.getSquareBrackets();
							maxSizeLimit = filter.getLimit();
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}

							buffer.append(filter.getAnonymizeMessage());

							if(metrics != null) {
								metrics.onAnonymize(1);
							}
							
							// adjust max size limit
							maxSizeLimit += offset - endQuoteIndex - 1 - anonymizeJsonValue.length;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							if(offset >= maxSizeLimit) {
								// make the previously flushed 'written mark' count
								start = offset;
								
								// TODO this might in some cases exclude the last field + value in a document 
								// which ends with a lot of whitespace but this is acceptable
								
								break loop;
							} else {
								mark = offset;
								start = offset;
							}

						}
						
					}

					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							// just remove whitespace
							CharArrayWhitespaceFilter.process(chars, offset, maxSizeLimit, buffer);

							return 0;
						}							
					}
					
					matches--;
					/*
					if(maxSizeLimit >= maxReadLimit) {
						// filtering only for full path, i.e. keep the rest of the document
						filter.setLevel(0);
						bracketLevel = 0;
						
						offset = rangesFullPath(chars, offset, maxReadLimit, level, elementPaths, matches, filterType, pathMatches, filter);
						
						break loop;
					}
					*/
				} else {
					start = nextOffset;
					offset = nextOffset;
				}

				continue;
			}
			offset++;
		}

		if(bracketLevel == 0) {
			buffer.append(chars, start, offset - start);
		} else {
			int markLimit = MaxSizeJsonFilter.markToLimit(mark, chars[mark]);

			if(start < markLimit) {
				buffer.append(chars, start, markLimit - start);
			} else {
				buffer.setLength(MaxSizeJsonFilter.markToLimit(writtenMark, buffer.charAt(writtenMark)));
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		}
		
		return level;
	}
	
	public boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}
		
		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);

		int bufferLength = output.size();

		int maxSizeLimit = offset + maxSize;
		
		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}

			ByteArrayWhitespaceBracketFilter filter = new ByteArrayWhitespaceBracketFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);

			filter.setLimit(maxSizeLimit);
			
			int level = processMaxSize(chars, offset, maxReadLimit, 0, stream, this.pathBytes, 0, filterType, maxPathMatches, filter, metrics);
			stream.writeTo(output);

			if(metrics != null) {
				metrics.onInput(length);
				
				if(level > 0) {
					metrics.onMaxSize(-1);
				}
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected int processMaxSize(final byte[] chars, int offset, int maxReadLimit, int level, final FlexibleOutputStream output, final byte[][] elementPaths, int matches, FilterType filterType, int pathMatches, ByteArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) throws IOException {
		int maxSizeLimit = filter.getLimit();

		int start = filter.getStart();

		int mark = filter.getMark();
		int writtenMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();
	
		loop:
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
					
					offset = filter.skipObjectOrArrayMaxSize(chars, offset + 1, maxReadLimit, output);
					
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
					
					if(filterType == FilterType.PRUNE) {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}
						}
						
						output.write(filter.getPruneMessage(), 0, filter.getPruneMessageLength());
						if(metrics != null) {
							metrics.onPrune(1);
						}
						
						maxSizeLimit += offset - endQuoteIndex - 1 - pruneJsonValueAsBytes.length;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						if(offset >= maxSizeLimit) {
							// make the previously flushed 'written mark' count
							start = offset;
							
							// TODO this might in some cases exclude the last field + value in a document 
							// which ends with a lot of whitespace but this is acceptable
							
							break loop;
						} else {
							mark = offset;
							start = offset;
						}						
					} else {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							
							filter.setLimit(maxSizeLimit);
							filter.setStart(nextOffset);
							filter.setLevel(bracketLevel);
							filter.setMark(mark);
							filter.setWrittenMark(writtenMark);
							
							offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset, maxReadLimit, output, metrics);
							
							start = filter.getStart();
							bracketLevel = filter.getLevel();
							mark = filter.getMark();
							writtenMark = filter.getWrittenMark();
							squareBrackets = filter.getSquareBrackets();
							maxSizeLimit = filter.getLimit();
							
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
							}

							output.write(filter.getAnonymizeMessage(), 0, filter.getAnonymizeMessageLength());

							if(metrics != null) {
								metrics.onAnonymize(1);
							}
							
							// adjust max size limit
							maxSizeLimit += offset - endQuoteIndex - 1 - anonymizeJsonValueAsBytes.length;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							if(offset >= maxSizeLimit) {
								// make the previously flushed 'written mark' count
								start = offset;
								
								// TODO this might in some cases exclude the last field + value in a document 
								// which ends with a lot of whitespace but this is acceptable
								
								break loop;
							} else {
								mark = offset;
								start = offset;
							}
						}
						
					}

					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							// just remove whitespace
							ByteArrayWhitespaceFilter.process(chars, offset, maxSizeLimit, output);

							return 0;
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
		
		if(bracketLevel == 0) {
			output.write(chars, start, offset - start);
		} else {
			int markLimit = MaxSizeJsonFilter.markToLimit(mark, chars[mark]);

			if(markLimit > start) {
				output.write(chars, start, markLimit - start);
			} else {
				output.setCount(MaxSizeJsonFilter.markToLimit(writtenMark, output.getByte(writtenMark)));
			}
			
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, output);
		}

		return level;
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
