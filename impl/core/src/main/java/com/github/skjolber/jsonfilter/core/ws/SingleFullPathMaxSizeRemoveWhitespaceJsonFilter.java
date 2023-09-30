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
			
			processMaxSize(chars, offset, maxReadLimit, 0, buffer, this.pathChars, filterType, maxPathMatches, filter, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected void processMaxSize(final char[] chars, int offset, int maxReadLimit, int level, final StringBuilder buffer, final char[][] elementPaths, FilterType filterType, int pathMatches, CharArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) {
		int maxSizeLimit = filter.getLimit();

		int flushedOffset = filter.getStart();
		int mark = filter.getMark();
		int streamMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();

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
					maxSizeLimit = maxReadLimit;
				}

				flushedOffset = offset;
				c = chars[offset];
			}
			switch(c) {
			case '{' :
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				offset++;
				
				squareBrackets[bracketLevel] = false;
				bracketLevel++;
				
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}

				mark = offset;
				level++;

				continue;
			case '}' :
				level--;
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case '[' : {
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				squareBrackets[bracketLevel] = true;
				bracketLevel++;

				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			}
			case ']' :
				bracketLevel--;
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
			case '"' :
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;

				if(flushedOffset <= mark) {
					streamMark = buffer.length() + mark - flushedOffset; 
				}
				buffer.append(chars, flushedOffset, endQuoteIndex - flushedOffset + 1);			
				
				flushedOffset = endQuoteIndex + 1;

				nextOffset++;
				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					// was a value
					flushedOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				buffer.append(':');

				nextOffset++;
				
				boolean match = elementPaths[level] == STAR_CHARS || matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level]);

				if(chars[nextOffset] <= 0x20) {
					offset = nextOffset;
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					maxSizeLimit += nextOffset - offset;

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				}
				
				if(!match) {
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;
							break loop;
						}
						
						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);

						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setWrittenMark(streamMark);
						
						offset = filter.skipObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, buffer);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushedOffset = filter.getStart();
						streamMark = filter.getWrittenMark();
						
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();						
					} else if(chars[nextOffset] == '"') {
						flushedOffset = nextOffset;
						
						nextOffset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						
						offset = nextOffset;
					} else {
						flushedOffset = nextOffset;
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					}
					continue;
				}

				if(level + 1 == elementPaths.length) {
					if(filterType == FilterType.PRUNE) {
						if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
							offset = nextOffset;
							break loop;
						}
						
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
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
						}
						
						mark = offset;
						flushedOffset = offset;
					} else {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							maxSizeLimit--;
							if(nextOffset >= maxSizeLimit) {
								offset = nextOffset;

								break loop;
							}

							squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
							bracketLevel++;

							if(bracketLevel >= squareBrackets.length) {
								squareBrackets = filter.grow(squareBrackets);
							}
							
							filter.setLimit(maxSizeLimit);
							filter.setStart(nextOffset);
							filter.setLevel(bracketLevel);
							filter.setMark(nextOffset + 1);
							filter.setWrittenMark(streamMark);
							
							offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, buffer, metrics);
							
							flushedOffset = filter.getStart();
							bracketLevel = filter.getLevel();
							mark = filter.getMark();
							streamMark = filter.getWrittenMark();
							squareBrackets = filter.getSquareBrackets();
							maxSizeLimit = filter.getLimit();
						} else {
							if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
								offset = nextOffset;
								
								break loop;
							}
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							buffer.append(filter.getAnonymizeMessage());

							if(metrics != null) {
								metrics.onAnonymize(1);
							}
							
							maxSizeLimit += offset - nextOffset - anonymizeJsonValue.length;

							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							mark = offset;
							flushedOffset = offset;
							
						}
					}

					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							// just remove whitespace
							
							MaxSizeRemoveWhitespaceJsonFilter.process(chars, offset, flushedOffset, buffer, maxReadLimit, maxSizeLimit, bracketLevel, squareBrackets, mark, streamMark, metrics);

							return;
						}							
					}
				} else {
					flushedOffset = nextOffset;
					offset = nextOffset;
				}

				continue;
			}
			offset++;
		}
		
		if(bracketLevel > 0) {
			if(flushedOffset <= mark) {
				streamMark = buffer.length() + mark - flushedOffset; 
			}
			buffer.append(chars, flushedOffset, offset - flushedOffset);
			flushedOffset = offset;
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						buffer.append(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				}
				buffer.setLength(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		} else {
			buffer.append(chars, flushedOffset, offset - flushedOffset);
		}		
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
			
			processMaxSize(chars, offset, maxReadLimit, 0, stream, this.pathBytes, 0, filterType, maxPathMatches, filter, metrics);

			stream.writeTo(output);

			if(metrics != null) {
				metrics.onInput(length);
				int written = output.size() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected void processMaxSize(final byte[] chars, int offset, int maxReadLimit, int level, final FlexibleOutputStream stream, final byte[][] elementPaths, int matches, FilterType filterType, int pathMatches, ByteArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) throws IOException {
		int maxSizeLimit = filter.getLimit();

		int flushedOffset = filter.getStart();
		int mark = filter.getMark();
		int streamMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();

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
					maxSizeLimit = maxReadLimit;
				}

				flushedOffset = offset;
				c = chars[offset];
			}
			switch(c) {
			case '{' :
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				offset++;
				
				squareBrackets[bracketLevel] = false;
				bracketLevel++;
				
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}

				mark = offset;
				level++;

				continue;
			case '}' :
				level--;
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case '[' : {
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				squareBrackets[bracketLevel] = true;
				bracketLevel++;

				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			}
			case ']' :
				bracketLevel--;
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
			case '"' :				
				
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;

				if(flushedOffset <= mark) {
					streamMark = stream.size() + mark - flushedOffset; 
				}
				stream.write(chars, flushedOffset, endQuoteIndex - flushedOffset + 1);			

				flushedOffset = endQuoteIndex + 1;
				
				nextOffset++;
				
				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					// was a value
					flushedOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				stream.write(':');

				nextOffset++;
				
				boolean match = elementPaths[level] == STAR_BYTES || matchPath(chars, offset + 1, endQuoteIndex, elementPaths[level]);

				if(chars[nextOffset] <= 0x20) {
					offset = nextOffset;
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					maxSizeLimit += nextOffset - offset;
					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				}
				
				if(!match) {
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;

							break loop;
						}
						
						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);

						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setWrittenMark(streamMark);
						
						offset = filter.skipObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, stream);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushedOffset = filter.getStart();
						streamMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();						
						
					} else if(chars[nextOffset] == '"') {
						flushedOffset = nextOffset;
						
						nextOffset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);

						offset = nextOffset;
					} else {
						flushedOffset = nextOffset;
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					}

					continue;
				}

				if(level + 1 == elementPaths.length) {
					if(filterType == FilterType.PRUNE) {
						if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
							offset = nextOffset;

							break loop;
						}
						
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
						} else {
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}
						}
						
						stream.write(filter.getPruneMessage());
						if(metrics != null) {
							metrics.onPrune(1);
						}
						
						// adjust max size limit
						maxSizeLimit += offset - nextOffset - pruneJsonValue.length;
						
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						mark = offset;
						flushedOffset = offset;
						
					} else {
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							maxSizeLimit--;
							if(nextOffset >= maxSizeLimit) {
								offset = nextOffset;
								
								break loop;
							}
							
							squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
							bracketLevel++;

							if(bracketLevel >= squareBrackets.length) {
								squareBrackets = filter.grow(squareBrackets);
							}
							
							filter.setLimit(maxSizeLimit);
							filter.setStart(nextOffset);
							filter.setLevel(bracketLevel);
							filter.setMark(nextOffset + 1);
							filter.setWrittenMark(streamMark);
							
							offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, stream, metrics);
							
							flushedOffset = filter.getStart();
							bracketLevel = filter.getLevel();
							mark = filter.getMark();
							streamMark = filter.getWrittenMark();
							squareBrackets = filter.getSquareBrackets();
							maxSizeLimit = filter.getLimit();
						} else {
							if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
								offset = nextOffset;
								
								break loop;
							}
							
							if(chars[nextOffset] == '"') {
								// quoted value
								offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							} else {
								offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							}

							stream.write(filter.getAnonymizeMessage());

							
							if(metrics != null) {
								metrics.onAnonymize(1);
							}
							
							maxSizeLimit += offset - nextOffset - anonymizeJsonValue.length;
							
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							mark = offset;
							flushedOffset = offset;

						}
					}

					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							// just remove whitespace
							MaxSizeRemoveWhitespaceJsonFilter.process(chars, offset, flushedOffset, stream, maxSizeLimit, maxReadLimit, bracketLevel, squareBrackets, mark, streamMark, metrics);

							return;
						}							
					}
				} else {
					flushedOffset = nextOffset;
					offset = nextOffset;
				}

				continue;
			}
			offset++;
			
		}

		if(bracketLevel > 0) {
			if(flushedOffset <= mark) {
				streamMark = stream.size() + mark - flushedOffset; 
			}
			stream.write(chars, flushedOffset, offset - flushedOffset);
			flushedOffset = offset;
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushedOffset) {
						stream.write(chars, flushedOffset, markLimit - flushedOffset);
					}
					break markLimit;
				}
				stream.setCount(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, stream);
		} else {
			stream.write(chars, flushedOffset, offset - flushedOffset);
		}
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
