package com.github.skjolber.jsonfilter.base;

public class CharArrayRangesMaxSizeWhitespaceFilter extends CharArrayRangesBracketFilter {

	private int start;
	private int writtenMark;
	private byte[] digit = new byte[11];
	
	public CharArrayRangesMaxSizeWhitespaceFilter(int initialCapacity, int length) {
		super(initialCapacity, length);
	}

	public void setWrittenMark(int writtenMark) {
		this.writtenMark = writtenMark;
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

}
