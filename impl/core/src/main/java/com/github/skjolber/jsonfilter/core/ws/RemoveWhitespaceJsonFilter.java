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

import java.util.Arrays;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class RemoveWhitespaceJsonFilter extends AbstractJsonFilter {

	public RemoveWhitespaceJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public RemoveWhitespaceJsonFilter() {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	protected RemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int bufferLength = buffer.length();
		
		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			
			CharArrayWhitespaceFilter.process(chars, offset, maxReadLimit, buffer);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(buffer.length() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			// do not leave partial output in the buffer
			buffer.setLength(bufferLength);
			return false;
		}
	}
	
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int bufferLength = output.size();
		
		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);

			ByteArrayWhitespaceFilter.process(chars, offset, maxReadLimit, output);
			
			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(output.size() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			// do not leave partial output in the buffer
			output.setSize(bufferLength);
			return false;
		}
	}

	@Override
	public String process(String jsonString) {
		return process(jsonString, (JsonFilterMetrics)null);
	}

	@Override
	public String process(String jsonString, JsonFilterMetrics metrics) {
		if(mustConstrainMaxSize(jsonString.length())) {
			// the subclass cuts the document
			return super.process(jsonString, metrics);
		}
		char[] chars = jsonString.toCharArray();
		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, chars.length);
			int whitespace = CharArrayWhitespaceFilter.findWhitespace(chars, 0, maxReadLimit);
			if(whitespace == chars.length) {
				// no whitespace to remove, as for most documents: the output is the input
				if(metrics != null) {
					metrics.onInput(chars.length);
					metrics.onOutput(chars.length);
				}
				return jsonString;
			}
			// continue from the first whitespace
			StringBuilder output = new StringBuilder(chars.length);
			output.append(chars, 0, whitespace);
			CharArrayWhitespaceFilter.process(chars, whitespace, maxReadLimit, output);

			if(metrics != null) {
				metrics.onInput(chars.length);
				metrics.onOutput(output.length());
			}
			return output.toString();
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		return process(chars, offset, length, (JsonFilterMetrics)null);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics metrics) {
		if(mustConstrainMaxSize(length)) {
			// the subclass cuts the document
			return super.process(chars, offset, length, metrics);
		}
		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			int whitespace = ByteArrayWhitespaceFilter.findWhitespace(chars, offset, maxReadLimit);
			if(whitespace == offset + length) {
				// no whitespace to remove: the output is a copy of the input, without the output stream's copy
				if(metrics != null) {
					metrics.onInput(length);
					metrics.onOutput(length);
				}
				return Arrays.copyOfRange(chars, offset, offset + length);
			}
			// continue from the first whitespace
			ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(length);
			output.write(chars, offset, whitespace - offset);
			ByteArrayWhitespaceFilter.process(chars, whitespace, maxReadLimit, output);

			if(metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(output.size());
			}
			return output.toByteArray();
		} catch(Exception e) {
			return null;
		}
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		return process(chars, offset, length, output, null);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}
	
	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
