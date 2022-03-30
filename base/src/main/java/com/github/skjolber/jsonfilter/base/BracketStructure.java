package com.github.skjolber.jsonfilter.base;

import java.io.IOException;
import java.io.OutputStream;

/**
 * For use in max size filters 
 * 
 */
public class BracketStructure {

	private boolean[] squareBrackets = new boolean[32];
	private int mark;
	private int level;
	
	public void increment(boolean squareBracket) {
		squareBrackets[level] = squareBracket;
		level++;
		
		if(level >= squareBrackets.length) {
			boolean[] next = new boolean[squareBrackets.length + 32];
			System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
			squareBrackets = next;
		}
	}
	
	public void decrement() {
		level--;
	}
	
	public void mark(int mark) {
		this.mark = mark;
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
	
	public void closeStructure(final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
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
	
	public void alignMark(char[] chars) {
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
	
	public void setSquareBrackets(boolean[] squareBrackets) {
		this.squareBrackets = squareBrackets;
	}
}
