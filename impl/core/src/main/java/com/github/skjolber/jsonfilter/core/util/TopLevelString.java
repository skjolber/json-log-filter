package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * Support for documents which are a single string value, like <code>"abc"</code>.
 * <br><br>
 * The regular filters expect a structural character after each string, to tell keys from values,
 * which a top-level string does not have. Rather than adding bounds checks to the hot path,
 * filters check for this rare case once per document and handle it here: paths cannot match,
 * so the string is only truncated (max string length) and surrounding whitespace optionally removed.
 */

public class TopLevelString {

	private TopLevelString() {
	}

	public static boolean isTopLevelString(final char[] chars, int offset, int length) {
		int limit = offset + length;
		while(offset < limit && chars[offset] <= 0x20) {
			offset++;
		}
		return offset < limit && chars[offset] == '"';
	}

	public static boolean isTopLevelString(final byte[] chars, int offset, int length) {
		int limit = offset + length;
		while(offset < limit && chars[offset] <= 0x20) {
			offset++;
		}
		return offset < limit && chars[offset] == '"';
	}

	/**
	 * Add ranges for a top-level string document.
	 *
	 * @param chars document
	 * @param offset document offset
	 * @param length document length
	 * @param maxStringLength max string length, excluding quotes
	 * @param removeWhitespace remove whitespace around the string
	 * @param filter filter to add ranges to
	 * @return the filter
	 */

	public static CharArrayRangesFilter ranges(final char[] chars, int offset, int length, int maxStringLength, boolean removeWhitespace, CharArrayRangesFilter filter) {
		int limit = offset + length;

		int start = offset;
		while(chars[start] <= 0x20) {
			start++;
		}
		int end = CharArrayRangesFilter.scanQuotedValue(chars, start); // closing quote

		if(removeWhitespace && start > offset) {
			filter.addDelete(offset, start);
		}
		if(end - start - 1 > maxStringLength) {
			int removeStart = start + 1 + maxStringLength;
			filter.addMaxLength(chars, removeStart, end, end - removeStart);
		}
		if(removeWhitespace && end + 1 < limit) {
			filter.addDelete(end + 1, limit);
		}
		return filter;
	}

	public static ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length, int maxStringLength, boolean removeWhitespace, ByteArrayRangesFilter filter) {
		int limit = offset + length;

		int start = offset;
		while(chars[start] <= 0x20) {
			start++;
		}
		int end = ByteArrayRangesFilter.scanQuotedValue(chars, start); // closing quote

		if(removeWhitespace && start > offset) {
			filter.addDelete(offset, start);
		}
		if(end - start - 1 > maxStringLength) {
			int removeStart = start + 1 + maxStringLength;
			filter.addMaxLength(chars, removeStart, end, end - removeStart);
		}
		if(removeWhitespace && end + 1 < limit) {
			filter.addDelete(end + 1, limit);
		}
		return filter;
	}

	public static void process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics, int maxStringLength, boolean removeWhitespace, char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		CharArrayRangesFilter filter = ranges(chars, offset, length, maxStringLength, removeWhitespace, new CharArrayRangesFilter(-1, length, pruneMessage, anonymizeMessage, truncateMessage));

		buffer.ensureCapacity(buffer.length() + filter.getMaxOutputLength());

		filter.filter(chars, offset, length, buffer, metrics);
	}

	public static void process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics, int maxStringLength, boolean removeWhitespace, byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
		ByteArrayRangesFilter filter = ranges(chars, offset, length, maxStringLength, removeWhitespace, new ByteArrayRangesFilter(-1, length, pruneMessage, anonymizeMessage, truncateMessage));

		filter.filter(chars, offset, length, buffer, metrics);
	}
}
