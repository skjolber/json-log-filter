package com.github.skjolber.jsonfilter.base;

public class CharArrayRangesBracketFilter extends CharArrayRangesFilter {

	protected BracketStructure bracketStructure = new BracketStructure();

	public CharArrayRangesBracketFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage,
			char[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public CharArrayRangesBracketFilter(int initialCapacity, int length) {
		super(initialCapacity, length);
	}

	@Override
	public void filter(char[] chars, int offset, int length, StringBuilder buffer) {
		super.filter(chars, offset, length, buffer);
		
		bracketStructure.closeStructure(buffer);
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
	
	
	public static int skipObject(char[] chars, int offset, int limit, BracketStructure bracketStructure) {
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

					mark = offset;

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

		return offset;
	}
	
	public BracketStructure getBracketStructure() {
		return bracketStructure;
	}

	public static int anonymizeSubtree(char[] chars, int offset, int limit, CharArrayRangesFilter filter, BracketStructure bracketStructure) {
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

					mark = offset;

					if(level == levelLimit) {
						offset++;
						break loop;
					}
					
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
				case ' ' : 
				case '\t' : 
				case '\n' : 
				case '\r' : {
					break;
				}
				case '"' : {
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					nextOffset++;
	
					// is this a field name or a value? A field name must be followed by a colon
					
					// special case: no whitespace
					if(chars[nextOffset] == ':') {
						// key
						offset = nextOffset + 1;
					} else {
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09 \t
						// carriage return: 0x0D \r
						// newline: 0x0A \n
						
						if(chars[nextOffset] > 0x20) {						
							// was a value
							if(offset + filter.getAnonymizeMessageLength() < limit) {
								int removedLength = filter.getRemovedLength();
								filter.addAnon(offset, nextOffset);
								limit += filter.getRemovedLength() - removedLength;
								
								mark = nextOffset;
							}

							offset = nextOffset;
						} else {
							// fast-forward over whitespace
							int end = nextOffset;
	
							// optimization: scan for highest value
							// space: 0x20
							// tab: 0x09
							// carriage return: 0x0D
							// newline: 0x0A
	
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);
							
							if(chars[nextOffset] == ':') {
								// key
								offset = nextOffset + 1;
							} else {
								// value
								if(offset + filter.getAnonymizeMessageLength() < limit) {
									int removedLength = filter.getRemovedLength();
									filter.addAnon(offset, end);
									limit += filter.getRemovedLength() - removedLength;
	
									mark = nextOffset;
								}
								offset = nextOffset;
							}
						}
					}
					
					continue;
				}
				default : {
					// scalar value
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');
					
					if(offset + filter.getAnonymizeMessageLength() < limit) {
						int removedLength = filter.getRemovedLength();
						filter.addAnon(offset, nextOffset);
						limit += filter.getRemovedLength() - removedLength;

						mark = nextOffset;
					}

					offset = nextOffset;

					continue;
							
				}
			}
			offset++;
		}
		
		bracketStructure.setLevel(level);
		bracketStructure.setMark(mark);

		return offset;
	}
	

	
}
