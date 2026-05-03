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

import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

/**
 * Max-size filter that produces the same output as {@link MaxSizeJsonFilter}
 * but finds the truncation cut point by scanning <em>backward</em> from the
 * end of the document rather than forward from the start.
 *
 * <p>This is an optimisation for documents that are only slightly larger than
 * {@code maxSize}: the backward scan covers only {@code length - maxSize}
 * characters, whereas the forward scan covers {@code maxSize} characters.
 * Both filters produce identical output for the same input and {@code maxSize}.
 *
 * <p>Algorithm: scan right-to-left tracking bracket depth.  At each
 * {@code }/]/{/[} or {@code ,} position, check whether
 * {@code proposedMark + bracketLevel <= offset + maxSize}.  The first such
 * position (rightmost valid cut) is used as the mark; the head
 * {@code chars[offset..mark)} is then emitted followed by
 * {@link MaxSizeJsonFilter#closeStructure closing brackets}.
 * A final {@link MaxSizeJsonFilter#markToLimit markToLimit} pass may extend
 * the mark to rescue a trailing value that fell just outside the main loop due
 * to whitespace.
 */
public class MaxSizeFromEndJsonFilter extends AbstractJsonFilter {

	public MaxSizeFromEndJsonFilter(String pruneMessage, String anonymizeMessage, String truncateMessage, int maxSize) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MaxSizeFromEndJsonFilter(int maxSize) {
		this(FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, maxSize);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		return process(chars, offset, length, buffer, null);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if (!mustConstrainMaxSize(length)) {
			if (chars.length < offset + length) {
				return false;
			}
			buffer.append(chars, offset, length);

			if (metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(length);
			}

			return true;
		}

		int bufferLength = buffer.length();

		try {
			processMaxSizeFromEnd(chars, offset, length, offset + maxSize, buffer);

			if (metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				int totalSize = length;
				if (written < totalSize) {
					metrics.onMaxSize(totalSize - written);
				}
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Core backward-scan implementation (char variant).
	 *
	 * @param maxSizeLimit  {@code offset + maxSize} — the same value passed by
	 *                      {@link MaxSizeJsonFilter#processMaxSize}
	 */
	protected static void processMaxSizeFromEnd(final char[] chars, int offset, int length, int maxSizeLimit, final StringBuilder buffer) {
		int end = offset + length;
		int flushOffset = offset;
		int pos = end - 1;

		int bracketLevel = 0;
		boolean[] squareBrackets = new boolean[32];

		int mark = offset;
		int markBracketLevel = 0;

		loop:
		while (pos >= offset) {
			switch (chars[pos]) {
				case '}':
				case ']': {
					int blBefore = bracketLevel;
					squareBrackets[bracketLevel] = (chars[pos] == ']');
					bracketLevel++;
					if (bracketLevel >= squareBrackets.length) {
						boolean[] next = new boolean[squareBrackets.length + 32];
						System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
						squareBrackets = next;
					}
					pos--;
					// Strict < because B_forward = B_backward+1; equivalence: pos+2+blBefore < maxSizeLimit
					if (pos + 2 + blBefore < maxSizeLimit) {
						mark = pos + 2;
						markBracketLevel = blBefore;
						break loop;
					}
					continue;
				}
				case '{':
				case '[': {
					int blBefore = bracketLevel;
					bracketLevel--;
					pos--;
					// <= because B_forward = B_backward for opening brackets (symmetric match)
					if (pos + 2 + blBefore <= maxSizeLimit) {
						mark = pos + 2;
						markBracketLevel = blBefore;
						break loop;
					}
					continue;
				}
				case ',':
					// Strict < mirrors the forward filter: the forward loop exits at offset==maxSizeLimit
					// before the comma, so a comma at exactly the budget boundary is excluded.
					if (pos + bracketLevel < maxSizeLimit) {
						mark = pos;
						markBracketLevel = bracketLevel;
						break loop;
					}
					break;
				case '"':
					pos = scanBackwardBeyondQuotedValue(chars, pos);
					break;
				default:
			}
			pos--;
		}

		if (markBracketLevel > 0) {
			// exitPos mirrors the forward scan's maxSizeLimit-adjusted exit position
			int exitPos = maxSizeLimit - markBracketLevel;
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, exitPos, end, exitPos, mark);
			if (markLimit != -1) {
				buffer.append(chars, flushOffset, markLimit - flushOffset);
			} else {
				buffer.append(chars, flushOffset, mark - flushOffset);
			}
			MaxSizeJsonFilter.closeStructure(markBracketLevel, squareBrackets, buffer);
		}
		// else: structured data with budget too tight for any content → output empty (matches forward filter)
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		return process(chars, offset, length, output, null);
	}

	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if (!mustConstrainMaxSize(length)) {
			if (chars.length < offset + length) {
				return false;
			}
			output.write(chars, offset, length);

			if (metrics != null) {
				metrics.onInput(length);
				metrics.onOutput(length);
			}

			return true;
		}

		int bufferLength = output.size();

		try {
			processMaxSizeFromEnd(chars, offset, length, offset + maxSize, output);

			if (metrics != null) {
				metrics.onInput(length);
				int written = output.size() - bufferLength;
				int totalSize = length;
				if (written < totalSize) {
					metrics.onMaxSize(totalSize - written);
				}
				metrics.onOutput(output.size() - bufferLength);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static void processMaxSizeFromEnd(byte[] chars, int offset, int length, int maxSizeLimit, ResizableByteArrayOutputStream output) throws IOException {
		int end = offset + length;
		int flushOffset = offset;
		int pos = end - 1;

		int bracketLevel = 0;
		boolean[] squareBrackets = new boolean[32];

		int mark = offset;
		int markBracketLevel = 0;

		loop:
		while (pos >= offset) {
			switch (chars[pos]) {
				case '}':
				case ']': {
					int blBefore = bracketLevel;
					squareBrackets[bracketLevel] = (chars[pos] == ']');
					bracketLevel++;
					if (bracketLevel >= squareBrackets.length) {
						boolean[] next = new boolean[squareBrackets.length + 32];
						System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
						squareBrackets = next;
					}
					pos--;
					if (pos + 2 + blBefore < maxSizeLimit) {
						mark = pos + 2;
						markBracketLevel = blBefore;
						break loop;
					}
					continue;
				}
				case '{':
				case '[': {
					int blBefore = bracketLevel;
					bracketLevel--;
					pos--;
					if (pos + 2 + blBefore <= maxSizeLimit) {
						mark = pos + 2;
						markBracketLevel = blBefore;
						break loop;
					}
					continue;
				}
				case ',':
					// Strict < mirrors the forward filter
					if (pos + bracketLevel < maxSizeLimit) {
						mark = pos;
						markBracketLevel = bracketLevel;
						break loop;
					}
					break;
				case '"':
					pos = scanBackwardBeyondQuotedValue(chars, pos);
					break;
				default:
			}
			pos--;
		}

		if (markBracketLevel > 0) {
			int exitPos = maxSizeLimit - markBracketLevel;
			int markLimit = MaxSizeJsonFilter.markToLimit(chars, exitPos, end, exitPos, mark);
			if (markLimit != -1) {
				output.write(chars, flushOffset, markLimit - flushOffset);
			} else {
				output.write(chars, flushOffset, mark - flushOffset);
			}
			MaxSizeJsonFilter.closeStructure(markBracketLevel, squareBrackets, output);
		}
		// else: structured data with budget too tight for any content → output empty (matches forward filter)
	}

	/**
	 * Scans backward from the closing {@code '"'} at {@code pos} and returns the
	 * position of the matching opening {@code '"'}.  Escape sequences
	 * ({@code \"}) are handled by counting consecutive backslashes.
	 */
	public static int scanBackwardBeyondQuotedValue(final char[] chars, int pos) {
		while (true) {
			while (chars[--pos] != '"');

			int slashOffset = pos - 1;
			while (slashOffset >= 0 && chars[slashOffset] == '\\') slashOffset--;

			// odd distance means zero or even backslashes → unescaped opening quote
			if ((pos - slashOffset) % 2 == 1) {
				return pos;
			}
		}
	}

	/**
	 * Byte-array variant of {@link #scanBackwardBeyondQuotedValue(char[], int)}.
	 */
	public static int scanBackwardBeyondQuotedValue(final byte[] chars, int pos) {
		while (true) {
			while (chars[--pos] != '"');

			int slashOffset = pos - 1;
			while (slashOffset >= 0 && chars[slashOffset] == '\\') slashOffset--;

			if ((pos - slashOffset) % 2 == 1) {
				return pos;
			}
		}
	}
}
