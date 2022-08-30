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

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class MaxStringLengthPrettyPrintJsonFilter extends AbstractJsonFilter {

	public MaxStringLengthPrettyPrintJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxStringLengthPrettyPrintJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected MaxStringLengthPrettyPrintJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {	
		length += offset;

		try {
			int start = offset;

			while(offset < length) {
				char c = chars[offset];
				if(c == '"') {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					if(nextOffset - offset - 1 > maxStringLength) {
						nextOffset++;

						// key or value, might be whitespace
						int end = nextOffset;

						// skip whitespace
						// optimization: scan for highest value
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(chars[nextOffset] == ':') {
							// was a key
							buffer.append(chars, start, end - start);
						} else {
							// was a value
							int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							buffer.append(chars, start, aligned - start);
							buffer.append(truncateStringValue);
							buffer.append(end - aligned - 1);
							buffer.append('"');
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
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		length += offset;

		byte[] digit = new byte[11];

		try {
			int start = offset;

			while(offset < length) {
				byte c = chars[offset];
				if(c == '"') {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

					if(nextOffset - offset - 1 > maxStringLength) {
						nextOffset++;

						// key or value, might be whitespace
						int end = nextOffset;

						// skip whitespace
						// optimization: scan for highest value
						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}

						if(chars[nextOffset] == ':') {
							// was a key
							output.write(chars, start, end - start);
						} else {

							// was a value
							int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
							output.write(chars, start, aligned - start);
							output.write(truncateStringValueAsBytes);
							ByteArrayRangesFilter.writeInt(output, end - aligned - 1, digit);
							output.write('"');
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
			return true;
		} catch(Exception e) {
			return false;
		}

	}
}
