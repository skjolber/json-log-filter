package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
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
		if(length == -1) {
			return process(reader, output);
		}
		
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
	
	public boolean process(Reader reader, StringBuilder output) throws IOException {
		char[] chars = new char[4 * 1024];

		int read;
		while(true) {
			read = reader.read(chars, 0, chars.length);
			if(read == -1) {
				break;
			}
			
			output.append(chars, 0, read);
		}
		
		return true;
	}

	@Override
	public byte[] process(byte[] chars) {
		return chars;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		output.write(chars, offset, length);
		return true;
	}

	@Override
	public boolean process(InputStream input, int length, ByteArrayOutputStream output) throws IOException {
		if(length == -1) {
			return process(input, output);
		}
		
		byte[] chars = new byte[4 * 1024];

		int offset = 0;
		int read;
		do {
			read = input.read(chars, 0, Math.min(chars.length, length - offset));
			if(read == -1) {
				throw new EOFException("Expected reader with " + length + " characters");
			}
			
			output.write(chars, 0, read);

			offset += read;
		} while(offset < length);
		
		return true;
	}

	public boolean process(InputStream input, ByteArrayOutputStream output) throws IOException {
		byte[] chars = new byte[4 * 1024];

		int read;
		while(true) {
			read = input.read(chars, 0, chars.length);
			if(read == -1) {
				break;
			}
			
			output.write(chars, 0, read);
		}
		
		return true;
	}

	@Override
	public boolean process(byte[] chars, ByteArrayOutputStream output) {
		return process(chars, 0, chars.length, output);
	}	
	
}
