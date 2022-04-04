package com.github.skjolber.jsonfilter.base;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayRangesBracketFilter extends ByteArrayRangesFilter {

	private boolean[] squareBrackets = new boolean[32];
	private int mark;
	private int level;
	
	public ByteArrayRangesBracketFilter(int initialCapacity, int length) {
		super(initialCapacity, length);
	}

	public ByteArrayRangesBracketFilter(int initialCapacity, int length, byte[] pruneMessage, byte[] anonymizeMessage,
			byte[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public boolean[] grow(boolean[] squareBrackets) {
		boolean[] next = new boolean[squareBrackets.length + 32];
		System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
		squareBrackets = next;
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
	
	public void alignMark(byte[] chars) {
		switch(chars[mark]) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				mark++;
				break;
			default : {
			}
		}
	}
	
	public void closeStructure(int level, OutputStream output) throws IOException {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				output.write(']');
			} else {
				output.write('}');
			}
		}
	}	

	public int skipObjectMaxSize(byte[] chars, int offset, int limit) {
		int levelLimit = getLevel() - 1;
		
		int level = getLevel();
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		loop:
		while(offset < limit) {

			switch(chars[offset]) {
				case '{' :
				case '[' :
				{
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = grow(squareBrackets);
					}
					mark = offset;
					
					break;
				}
				case ']' :
				case '}' : {
					level--;
					
					if(level == levelLimit) {
						offset++;
						break loop;
					}
					break;
				}
				case ',' :
					mark = offset;
					break;
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					break;
				}
				default : // do nothing
			}
			offset++;
		}

		setLevel(level);
		setMark(mark);
		setSquareBrackets(squareBrackets);

		return offset;
	}	
	

	public int skipSubtreeMaxSize(char[] chars, int offset, int limit) {
		
		int levelLimit = getLevel();
		
		int level = getLevel();
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = grow(squareBrackets);
					}
					mark = offset;
					
					break;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == levelLimit) {
						offset++;
						break loop;
					} else if(level < levelLimit) { // was scalar value
						break loop;
					}
					break;
				}
				case ',' : {
					mark = offset;
					if(level == levelLimit) { // was scalar value
						break loop;
					}
					break;
				}
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					
					if(level == levelLimit) { 
						offset++;
						break loop;
					}
					break;
				}
				default :
			}
			offset++;
		}
		
		setLevel(level);
		setMark(mark);
		setSquareBrackets(squareBrackets);
		
		return offset;
	}

	public void setSquareBrackets(boolean[] squareBrackets) {
		this.squareBrackets = squareBrackets;
	}
}
