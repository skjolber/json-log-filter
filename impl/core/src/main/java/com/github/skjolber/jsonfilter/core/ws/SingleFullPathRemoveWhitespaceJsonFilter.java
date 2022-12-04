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
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharWhitespaceFilter;

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
		
		CharWhitespaceFilter filter = new CharWhitespaceFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		
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
				
				switch(c) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						
						filter.setStart(start);
						
						offset = filter.skipObject(chars, offset + 1, limit, buffer);
						
						start = filter.getStart();
						
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

					buffer.append(chars, start, endQuoteIndex - start + 1);
					
					if(chars[nextOffset] != ':') {
						// was a value
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					// was a field name
					if(matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset + 1;
						start = nextOffset;
						
						continue;
					}
					
					if(matches == elementPaths.length) {
						start = nextOffset;
						
						buffer.append(':');
						
						nextOffset++;
						
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								// skip both whitespace and actual content
								offset = CharArrayRangesFilter.skipObject(chars, nextOffset);

								buffer.append(pruneJsonValue);
								
								start = offset;
							} else {
								filter.setStart(start);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, buffer);
								
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
							} else {
								buffer.append(anonymizeJsonValue);
							}
							
							start = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// just remove whitespace
								CharWhitespaceFilter.process(chars, nextOffset, limit, buffer);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(buffer.length() - bufferLength);
								}
								
								return true;
							}							
						}
						
						matches--;
					} else {
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

	public boolean process2(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		
		CharWhitespaceFilter filter = new CharWhitespaceFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		
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
				
				switch(c) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						
						filter.setStart(start);
						
						offset = filter.skipObject(chars, offset + 1, limit, buffer);
						
						start = filter.getStart();
						
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

					buffer.append(chars, start, endQuoteIndex - start + 1);

					if(chars[nextOffset] == ':') {
						// was a key

						if(matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
							matches++;
						} else {
							offset = nextOffset + 1;
							start = nextOffset;
							
							continue;
						}
						
						if(matches == elementPaths.length) {
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								if(filterType == FilterType.PRUNE) {
									// skip both whitespace and actual content
									offset = CharArrayRangesFilter.skipObject(chars, nextOffset);

									buffer.append(pruneJsonValue);
									
									start = offset;
								} else {
									filter.setStart(start);

									offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, buffer);
									
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
								} else {
									buffer.append(anonymizeJsonValue);
								}
							}
							
							if(pathMatches != -1) {
								pathMatches--;
								if(pathMatches == 0) {
									// just remove whitespace
									filter.process(chars, nextOffset, limit, buffer);
									
									if(metrics != null) {
										metrics.onInput(length);
										metrics.onOutput(buffer.length() - bufferLength);
									}
									
									return true;
								}							
							}
							
							matches--;
						} else {
							offset = nextOffset;
						}
					} else {
						// was a value
						buffer.append(chars, start, endQuoteIndex - start + 1);
						
						start = nextOffset;
					}

					offset = nextOffset;

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

	public boolean process3(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int limit = length + offset;
		
		int bufferLength = output.size();

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
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		
		ByteWhitespaceFilter filter = new ByteWhitespaceFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
		
		int bufferLength = output.size();
		
		int limit = length + offset;
		
		int level = 0;
		final byte[][] elementPaths = this.pathBytes;
		int matches = 0;
		FilterType filterType = this.filterType;
		int pathMatches = 0;

		try {
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

					continue;
				}
				
				switch(c) {
				case '{' :
					level++;
					
					if(level > matches + 1) {
						// so always level < elementPaths.length
						
						filter.setStart(start);
						
						offset = filter.skipObject(chars, offset + 1, limit, output);
						
						start = filter.getStart();
						
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

					output.write(chars, start, endQuoteIndex - start + 1);
					
					if(chars[nextOffset] != ':') {
						// was a value
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					// was a field name
					if(matchPath(chars, offset + 1, endQuoteIndex, elementPaths[matches])) {
						matches++;
					} else {
						offset = nextOffset + 1;
						start = nextOffset;
						
						continue;
					}
					
					if(matches == elementPaths.length) {
						start = nextOffset;
						
						output.write(':');
						
						nextOffset++;
						
						if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
							if(filterType == FilterType.PRUNE) {
								// skip both whitespace and actual content
								offset = ByteArrayRangesFilter.skipObject(chars, nextOffset);

								output.write(pruneJsonValueAsBytes);
								
								start = offset;
							} else {
								filter.setStart(start);

								offset = filter.anonymizeObjectOrArray(chars, nextOffset + 1, limit, output);
								
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
							} else {
								output.write(anonymizeJsonValueAsBytes);
							}
							
							start = offset;
						}
						
						if(pathMatches != -1) {
							pathMatches--;
							if(pathMatches == 0) {
								// just remove whitespace
								ByteWhitespaceFilter.process(chars, nextOffset, limit, output);
								
								if(metrics != null) {
									metrics.onInput(length);
									metrics.onOutput(output.size() - bufferLength);
								}
								
								return true;
							}							
						}
						
						matches--;
					} else {
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
