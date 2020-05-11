package com.github.skjolber.jsonfilter.base;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

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
	public boolean process(Reader reader, int length, StringBuilder output) throws IOException {
		char[] chars = new char[4 * 1024];

		int offset = 0;
		int read;
		do {
			read = reader.read(chars, 0, Math.min(chars.length, length - offset));
			if(read == -1) {
				throw new EOFException("Expected reader with " + length + " characters");
			}
			
			output.append(chars, 0, read);

			offset += read;
		} while(offset < length);
		
		return true;
	}
}
