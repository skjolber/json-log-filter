package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractJsonFilter implements JsonFilter {

	public static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";

	public static final String FILTER_PRUNE_MESSAGE_JSON = '"' + FILTER_PRUNE_MESSAGE + '"';
	public static final String FILTER_ANONYMIZE_JSON = '"' + FILTER_ANONYMIZE + '"';

	protected static final int MAX_STRING_LENGTH = Integer.MAX_VALUE - 2;

	protected final int maxStringLength; // not always in use, if so set to max int
	
	protected final char[] pruneJsonValue;
	protected final char[] anonymizeJsonValue;
	protected final char[] truncateStringValue;
	
	protected final byte[] pruneJsonValueAsBytes;
	protected final byte[] anonymizeJsonValueAsBytes;
	protected final byte[] truncateStringValueAsBytes;
	
	public AbstractJsonFilter(int maxStringLength, String pruneJson, String anonymizeJson, String truncateJsonString) {
		if(maxStringLength < -1 || maxStringLength > MAX_STRING_LENGTH) {
			throw new IllegalArgumentException("Expected -1 or positive integer lower than Integer.MAX_VALUE - 1");
		}
		
		if(maxStringLength == -1) {
			this.maxStringLength = MAX_STRING_LENGTH; // make room for quotes, without overflow
		} else {
			this.maxStringLength = maxStringLength;
		}
		
		this.pruneJsonValue = pruneJson.toCharArray();
		this.anonymizeJsonValue = anonymizeJson.toCharArray();
		this.truncateStringValue = truncateJsonString.toCharArray();
		
		this.pruneJsonValueAsBytes = pruneJson.getBytes(StandardCharsets.UTF_8);
		this.anonymizeJsonValueAsBytes = anonymizeJson.getBytes(StandardCharsets.UTF_8);
		this.truncateStringValueAsBytes = truncateJsonString.getBytes(StandardCharsets.UTF_8);
	}

	public boolean process(String jsonString, StringBuilder output) {
		char[] chars = jsonString.toCharArray();
		
		return process(chars, 0, chars.length, output);
	}
	
	public String process(char[] chars) {
		StringBuilder output = new StringBuilder(chars.length);
		
		if(process(chars, 0, chars.length, output)) {
			return output.toString();
		}
		return null;
	}
	
	public String process(String jsonString) {
		return process(jsonString.toCharArray());
	}
	
	public boolean process(Reader reader, int length, StringBuilder output) throws IOException {
		if(length == -1) {
			return process(reader, output);
		}
		char[] chars = new char[length];

		int offset = 0;
		while(offset < length) {
			int read = reader.read(chars, offset, length - offset);
			if(read == -1) {
				throw new EOFException("Expected reader with " + length + " characters");
			}

			offset += read;
		}

		return process(chars, 0, chars.length, output);
	}
	
	public boolean process(Reader reader, StringBuilder output) throws IOException {
		char[] chars = new char[4 * 1024];

		CharArrayWriter writer = new CharArrayWriter(chars.length);
		int read;
		do {
			read = reader.read(chars, 0, chars.length);
			if(read == -1) {
				break;
			}
			
			writer.write(chars, 0, read);
		} while(true);

		return process(writer.toString(), output);
	}
	
	@Override
	public byte[] process(byte[] chars) {
		ByteArrayOutputStream output = new ByteArrayOutputStream(chars.length);
		
		if(process(chars, 0, chars.length, output)) {
			return output.toByteArray();
		}
		return null;
	}

	@Override
	public boolean process(InputStream input, int length, ByteArrayOutputStream output) throws IOException {
		if(length == -1) {
			return process(input, output);
		}
		byte[] chars = new byte[length];

		int offset = 0;
		
		while(offset < length) {
			int read = input.read(chars, offset, length - offset);
			if(read == -1) {
				throw new EOFException("Expected stream with " + length + " characters");
			}

			offset += read;
		}

		return process(chars, 0, chars.length, output);
	}

	@Override
	public boolean process(InputStream input, ByteArrayOutputStream output) throws IOException {
		byte[] chars = new byte[4 * 1024];

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int read;
		do {
			read = input.read(chars, 0, chars.length);
			if(read == -1) {
				break;
			}
			
			bout.write(chars, 0, read);
		} while(true);

		byte[] bytes = bout.toByteArray();
		
		return process(bytes, 0, bytes.length, output);
	}
	
	@Override
	public boolean process(byte[] chars, ByteArrayOutputStream output) {
		return process(chars, 0, chars.length, output);
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}
	
	protected CharArrayRangesFilter getCharArrayRangesFilter() {
		return getCharArrayRangesFilter(-1);
	}

	protected CharArrayRangesFilter getCharArrayRangesFilter(int capacity) {
		return new CharArrayRangesFilter(capacity, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
	}
	
	protected ByteArrayRangesFilter getByteArrayRangesFilter() {
		return getByteArrayRangesFilter(-1);
	}
	
	protected ByteArrayRangesFilter getByteArrayRangesFilter(int capacity) {
		return new ByteArrayRangesFilter(capacity, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
	}
	
	/**
	 * Method that will quote text contents using JSON standard quoting,
	 * and append results to a supplied {@link StringBuilder}.
	 * Use this variant if you have e.g. a {@link StringBuilder} and want to avoid superfluous copying of it.
	 *
	 * @since 2.8
	 */
	public static void quoteAsString(CharSequence input, StringBuilder output) {
		final int[] escCodes = sOutputEscapes128;
		final int escCodeCount = escCodes.length;
		int inPtr = 0;
		final int inputLen = input.length();
		char[] qbuf = null;

		outer:
		while (inPtr < inputLen) {
			tight_loop:
			while (true) {
				char c = input.charAt(inPtr);
				if (c < escCodeCount && escCodes[c] != 0) {
					break tight_loop;
				}
				output.append(c);
				if (++inPtr >= inputLen) {
					break outer;
				}
			}
			// something to escape; 2 or 6-char variant?
			if (qbuf == null) {
				qbuf = _qbuf();
			}
			char d = input.charAt(inPtr++);
			int escCode = escCodes[d];
			int length = (escCode < 0)
					? _appendNumeric(d, qbuf)
					: _appendNamed(escCode, qbuf);
			output.append(qbuf, 0, length);
		}
	}
	
	private static int _appendNumeric(int value, char[] qbuf) {
		qbuf[1] = 'u';
		// We know it's a control char, so only the last 2 chars are non-0
		qbuf[4] = HC[value >> 4];
		qbuf[5] = HC[value & 0xF];
		return 6;
	}

	private static int _appendNamed(int esc, char[] qbuf) {
		qbuf[1] = (char) esc;
		return 2;
	}	
	
	/**
	 * Lookup table used for determining which output characters in
	 * 7-bit ASCII range need to be quoted.
	 */
	private final static int[] sOutputEscapes128;
	static {
		int[] table = new int[128];
		// Control chars need generic escape sequence
		for (int i = 0; i < 32; ++i) {
			// 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
			table[i] = -1;
		}
		// Others (and some within that range too) have explicit shorter sequences
		table['"'] = '"';
		table['\\'] = '\\';
		// Escaping of slash is optional, so let's not add it
		table[0x08] = 'b';
		table[0x09] = 't';
		table[0x0C] = 'f';
		table[0x0A] = 'n';
		table[0x0D] = 'r';
		sOutputEscapes128 = table;
	}
	
	private final static char[] HC = "0123456789ABCDEF".toCharArray();
	private final static byte[] HB;
	static {
		int len = HC.length;
		HB = new byte[len];
		for (int i = 0; i < len; ++i) {
			HB[i] = (byte) HC[i];
		}
	}   
	
	private static char[] _qbuf() {
		char[] qbuf = new char[6];
		qbuf[0] = '\\';
		qbuf[2] = '0';
		qbuf[3] = '0';
		return qbuf;
	}

}
