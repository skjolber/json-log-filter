package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilter;

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
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		output.write(chars, offset, length);
		return false;
	}

}
