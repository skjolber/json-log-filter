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
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharWhitespaceFilter;

public class MultiPathMaxStringLengthRemoveWhitespaceJsonFilter  extends AbstractMultiPathJsonFilter {

	public MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes,
			String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
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
		
		PathItem pathItem = this.pathItem;

		int level = 0;
		int pathMatches = 0;
		
		try {
			int limit = CharWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
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
					if(anyElementFilters == null && level > pathItem.getLevel()) {
						filter.setStart(start);
						
						offset = filter.skipObjectMaxStringLength(chars, offset + 1, limit, maxStringLength, buffer, metrics);
						
						start = filter.getStart();

						level--;
						
						continue;
					}
					break;
				case '}' :
					pathItem = pathItem.constrain(level);
					
					level--;
					
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

					if(chars[nextOffset] != ':') {
						// was a value
						if(endQuoteIndex - offset - 1 > maxStringLength) {
							int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
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
						
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					buffer.append(chars, start, endQuoteIndex - start + 1);

					FilterType filterType = null;
					
					// match again any higher filter
					pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, endQuoteIndex);
					if(pathItem.hasType()) {
						// matched
						filterType = pathItem.getType();
						
						pathItem = pathItem.constrain(level);
					}
					
					if(anyElementFilters != null && filterType == null) {
						filterType = matchAnyElements(chars, offset + 1, endQuoteIndex);
					}
					
					// was a field name
					if(filterType != null) {

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
	
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		ByteWhitespaceFilter filter = new ByteWhitespaceFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
		
		int bufferLength = output.size();

		PathItem pathItem = this.pathItem;

		int level = 0;
		int pathMatches = 0;

		try {
			int limit = ByteWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);

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
					
					if(anyElementFilters == null && level > pathItem.getLevel()) {
						filter.setStart(start);
						
						offset = filter.skipObjectMaxStringLength(chars, offset + 1, limit, maxStringLength, output, metrics);
						
						start = filter.getStart();

						level--;
						
						continue;
					}
					break;
				case '}' :
					
					pathItem = pathItem.constrain(level);
					
					level--;
					
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

					if(chars[nextOffset] != ':') {
						// was a value
						if(endQuoteIndex - offset - 1 > maxStringLength) {
							// was a value
							int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							output.write(chars, start, aligned - start);
							output.write(truncateStringValueAsBytes);
							ByteArrayRangesFilter.writeInt(output, endQuoteIndex - aligned, filter.getDigit());
							output.write('"');							
							
							if(metrics != null) {
								metrics.onMaxStringLength(1);
							}
						} else {
							output.write(chars, start, endQuoteIndex - start + 1);
						}
						
						offset = nextOffset;
						start = nextOffset;
						
						continue;
					}

					output.write(chars, start, endQuoteIndex - start + 1);
					
					FilterType filterType = null;
					
					// match again any higher filter
					pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, endQuoteIndex);
					if(pathItem.hasType()) {
						// matched
						filterType = pathItem.getType();
						
						pathItem = pathItem.constrain(level);
					}
					
					if(anyElementFilters != null && filterType == null) {
						filterType = matchAnyElements(chars, offset + 1, endQuoteIndex);
					}
					
					// was a field name
					if(filterType != null) {
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
