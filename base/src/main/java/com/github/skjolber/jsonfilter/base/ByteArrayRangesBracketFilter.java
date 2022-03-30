package com.github.skjolber.jsonfilter.base;

public class ByteArrayRangesBracketFilter extends ByteArrayRangesFilter {

	protected BracketStructure BracketStructure = new BracketStructure();
	
	public ByteArrayRangesBracketFilter(int initialCapacity, int length) {
		super(initialCapacity, length);
	}


	public ByteArrayRangesBracketFilter(int initialCapacity, int length, byte[] pruneMessage, byte[] anonymizeMessage,
			byte[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}


	public static int skipObject(byte[] chars, int offset, int limit, BracketStructure bracketStructure) {
		int levelLimit = bracketStructure.getLevel() - 1;
		
		int level = bracketStructure.getLevel();
		
		boolean[] squareBrackets = bracketStructure.getSquareBrackets();
		int mark = bracketStructure.getMark();

		loop:
		while(offset < limit) {

			switch(chars[offset]) {
				case '{' :
				case '[' :
				{
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = bracketStructure.grow(squareBrackets);
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

		bracketStructure.setLevel(level);
		bracketStructure.setMark(mark);
		bracketStructure.setSquareBrackets(squareBrackets);

		return offset;
	}	
	

	public static int skipSubtree(char[] chars, int offset, int limit, BracketStructure bracketStructure) {
		
		int levelLimit = bracketStructure.getLevel();
		
		int level = bracketStructure.getLevel();
		
		boolean[] squareBrackets = bracketStructure.getSquareBrackets();
		int mark = bracketStructure.getMark();

		loop:
		while(offset < limit) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = bracketStructure.grow(squareBrackets);
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
		
		bracketStructure.setLevel(level);
		bracketStructure.setMark(mark);
		bracketStructure.setSquareBrackets(squareBrackets);
		
		return offset;
	}
	
}
