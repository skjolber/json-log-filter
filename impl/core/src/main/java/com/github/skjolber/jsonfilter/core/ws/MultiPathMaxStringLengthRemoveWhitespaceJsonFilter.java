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
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class MultiPathMaxStringLengthRemoveWhitespaceJsonFilter extends AbstractJsonFilter {

	public MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
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

		try {
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
					} while(offset < limit && chars[offset] <= 0x20);					

					start = offset;

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
							int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							output.write(chars, start, aligned - start);
							output.write(truncateStringValueAsBytes);
							ByteArrayRangesFilter.writeInt(output, endQuoteIndex - aligned, digit);
							output.write('"');
							
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
					output.write(chars, start, offset - start);
					do {
						offset++;
					} while(offset < limit && chars[offset] <= 0x20);					

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

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
