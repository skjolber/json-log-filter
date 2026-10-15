package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * 
 * Default (noop) filter.
 *
 */

public class DefaultJsonFilter implements JsonFilter {

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics filterMetrics) {
		buffer.append(chars, offset, length);
		record(filterMetrics, length);
		return true;
	}

	@Override
	public String process(char[] chars, JsonFilterMetrics filterMetrics) {
		record(filterMetrics, chars.length);
		return new String(chars);
	}

	@Override
	public String process(String chars, JsonFilterMetrics filterMetrics) {
		record(filterMetrics, chars.length());
		return chars;
	}

	@Override
	public boolean process(String chars, StringBuilder output, JsonFilterMetrics filterMetrics) {
		output.append(chars);
		record(filterMetrics, chars.length());
		return true;
	}
	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics filterMetrics) {
		record(filterMetrics, chars.length);
		return chars;
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics filterMetrics) {
		byte[] output = new byte[length];
		System.arraycopy(chars, offset, output, 0, length);
		record(filterMetrics, length);
		return output;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
		output.write(chars, offset, length);
		record(filterMetrics, length);
		return true;
	}

	private static void record(JsonFilterMetrics filterMetrics, int length) {
		if(filterMetrics != null) {
			filterMetrics.onInput(length);
			filterMetrics.onOutput(length);
		}
	}
}
