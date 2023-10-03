package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * 
 * Default (noop) filter.
 *
 */

public class DefaultJsonFilter implements JsonFilter {

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		buffer.append(chars, offset, length);
		return true;
	}

	@Override
	public String process(char[] chars) {
		return new String(chars);
	}

	@Override
	public String process(String chars) {
		return chars;
	}

	@Override
	public boolean process(String chars, StringBuilder output) {
		output.append(chars);
		return true;
	}
	@Override
	public byte[] process(byte[] chars) {
		return chars;
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		byte[] output = new byte[length];
		System.arraycopy(chars, offset, output, 0, length);
		return output;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		output.write(chars, offset, length);
		return true;
	}

	@Override
	public String process(char[] chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public String process(String chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public boolean process(String chars, StringBuilder output, JsonFilterMetrics filterMetrics) {
		return process(chars, output);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

}
