package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteWhitespaceFilter {

	protected static final char[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	protected static final char[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_ANONYMIZE_MESSAGE.toCharArray();
	protected static final char[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;

	protected int start;
	protected int mark;
	protected int writtenMark;
	protected byte[] digit = new byte[11];
	
	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	

	public ByteWhitespaceFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public ByteWhitespaceFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getWrittenMark() {
		return writtenMark;
	}

	public void setWrittenMark(int writtenMark) {
		this.writtenMark = writtenMark;
	}
	
	public boolean[] grow(boolean[] squareBrackets) {
		boolean[] next = new boolean[squareBrackets.length + 32];
		System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
		this.squareBrackets = next;
		return next;
	}

	public boolean[] getSquareBrackets() {
		return squareBrackets;
	}
	
	public int getLevel() {
		return level;
	}
	
	public int getMark() {
		return mark;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setMark(int mark) {
		this.mark = mark;
	}

	public void closeStructure(final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
	}

	public int markToLimit(char[] chars) {
		switch(chars[mark]) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return mark + 1;
			default : {
				return mark;
			}
		}
	}

	public byte[] getDigit() {
		return digit;
	}

	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public static void process(byte[] chars, int offset, int limit, ByteArrayOutputStream output) {
		int start = offset;
		
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, start, offset - start);
				do {
					offset++;
				} while(offset < limit && chars[offset] <= 0x20);
				
				start = offset;
				
				continue;
			}
			offset++;
		}
		output.write(chars, start, offset - start);
	}

}
