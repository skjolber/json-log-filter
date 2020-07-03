package com.github.skjolber.jsonfilter.base;

import java.io.CharArrayWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractJsonFilter implements JsonFilter {

	public static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";

	protected final int maxStringLength; // not always in use, if so set to max int
	
	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;
	
	public AbstractJsonFilter() {
		this(-1, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE) ;
	}

	public AbstractJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		if(maxStringLength < -1 || maxStringLength > Integer.MAX_VALUE - 2) {
			throw new IllegalArgumentException("Expected -1 or positive integer lower than Integer.MAX_VALUE - 1");
		}
		
		if(maxStringLength == -1) {
			this.maxStringLength = Integer.MAX_VALUE - 2; // make room for quotes, without overflow
		} else {
			this.maxStringLength = maxStringLength;
		}
		
		this.pruneMessage = ('"' + pruneMessage + '"').toCharArray();
		this.anonymizeMessage = ('"' + anonymizeMessage + '"').toCharArray();
		this.truncateMessage = truncateMessage.toCharArray();
		
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
		int read;
		do {
			read = reader.read(chars, offset, length - offset);
			if(read == -1) {
				throw new EOFException("Expected reader with " + length + " characters");
			}

			offset += read;
		} while(offset < length);

		return process(chars, 0, chars.length, output);
	}
	
	public boolean process(Reader reader, StringBuilder output) throws IOException {
		char[] chars = new char[4 * 1024];

		CharArrayWriter writer = new CharArrayWriter(chars.length);
		int offset = 0;
		int read;
		do {
			read = reader.read(chars, offset, chars.length);
			if(read == -1) {
				break;
			}
			
			writer.write(chars, 0, read);

			offset += read;
		} while(true);

		return process(writer.toString(), output);
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}
}
