/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
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
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

/**
 * Max-size filter that automatically selects between scanning from the start
 * ({@link MaxSizeJsonFilter}) and scanning from the end
 * ({@link MaxSizeFromEndJsonFilter}) to minimise the number of characters
 * that must be processed.
 *
 * <ul>
 *   <li>Forward scan ({@link MaxSizeJsonFilter}) processes approximately
 *       {@code maxSize} characters.</li>
 *   <li>Backward scan ({@link MaxSizeFromEndJsonFilter}) processes
 *       approximately {@code length - maxSize} characters.</li>
 * </ul>
 *
 * <p>The cheaper scan is chosen: forward when {@code maxSize <= length - maxSize}
 * (i.e. the document is at least twice the limit), backward otherwise.
 * Both scans produce identical output for the same input.
 */
public class CompositeMaxSizeJsonFilter extends AbstractJsonFilter {

	private final MaxSizeJsonFilter forwardFilter;
	private final MaxSizeFromEndJsonFilter backwardFilter;

	public CompositeMaxSizeJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
		this.forwardFilter = new MaxSizeJsonFilter(pruneMessage, anonymizeMessage, truncateMessage, maxSize);
		this.backwardFilter = new MaxSizeFromEndJsonFilter(pruneMessage, anonymizeMessage, truncateMessage, maxSize);
	}

	public CompositeMaxSizeJsonFilter(int maxSize) {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, maxSize);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		return process(chars, offset, length, buffer, null);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if (!mustConstrainMaxSize(length) || maxSize <= length - maxSize) {
			return forwardFilter.process(chars, offset, length, buffer, metrics);
		} else {
			return backwardFilter.process(chars, offset, length, buffer, metrics);
		}
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if (!mustConstrainMaxSize(length) || maxSize <= length - maxSize) {
			return forwardFilter.process(chars, offset, length, output, metrics);
		} else {
			return backwardFilter.process(chars, offset, length, output, metrics);
		}
	}
}
