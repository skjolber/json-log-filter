package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class CharArrayRangesBracketFilter extends CharArrayRangesFilter {

	protected boolean[] squareBrackets = new boolean[32];
	protected int mark;
	protected int level;

	public CharArrayRangesBracketFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage,
			char[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public CharArrayRangesBracketFilter(int initialCapacity, int length) {
		super(initialCapacity, length);
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
	
	@Override
	public void filter(char[] chars, int offset, int length, StringBuilder buffer, JsonFilterMetrics metrics) {
		super.filter(chars, offset, length, buffer, metrics);
		
		closeStructure(buffer);
	}
	
	@Override
	public void filter(char[] chars, int offset, int length, StringBuilder buffer) {
		super.filter(chars, offset, length, buffer);
		
		closeStructure(buffer);
	}
	
	public int skipObjectMaxSize(char[] chars, int offset, int limit) {
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

		setLevel(level);
		setMark(mark);

		return offset;
	}
	
	public int skipObjectMaxSizeMaxStringLength(char[] chars, int offset, int maxSizeLimit, int limit, int maxStringLength) {
		int level = getLevel();
		int levelLimit = level - 1;
		
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		loop:
		while(offset < maxSizeLimit) {
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
					
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					nextOffset++;

					if(nextOffset - offset <= maxStringLength) {
						offset = nextOffset;
						
						continue;
					}
					// is this a field name or a value? A field name must be followed by a colon
					
					// special case: no whitespace
					if(chars[nextOffset] == ':') {
						// key
						offset = nextOffset + 1;
						
						continue;
					} else {
						// most likely there is now no whitespace, but a comma, end array or end object
						
						// legal whitespaces are:
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A
						
						int end = nextOffset - 1;
						if(chars[nextOffset] <= 0x20) {
							// fast-forward over whitespace
							// optimization: scan for highest value
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);

							if(chars[nextOffset] == ':') {
								// was a key
								offset = nextOffset + 1;
								continue;
							}
						}
						
						if(offset + maxStringLength >= maxSizeLimit) {
							offset = maxSizeLimit;
							
							break loop;
						}

						int removedLength = getRemovedLength();
						addMaxLength(chars, offset + maxStringLength - 1, end, -(offset + maxStringLength - end - 1));
						// increment limit since we removed something
						maxSizeLimit += getRemovedLength() - removedLength;

						if(maxSizeLimit > limit) {
							maxSizeLimit = limit;
						}
						
						if(nextOffset >= maxSizeLimit) {
							removeLastFilter();
							
							offset = maxSizeLimit;
							
							break loop;
						}
						
						offset = nextOffset;
						
						mark = offset;
					}
					
					continue;
				}
				default : // do nothing
			}
			offset++;
		}
		
		setLevel(level);
		setMark(mark);

		return offset;
	}

	public int anonymizeSubtree(char[] chars, int offset, int limit) {
		int levelLimit = getLevel();
		
		int level = getLevel();
		
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		// TODO identify scalar or tree value, pass flow accordingly
		
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

					mark = offset;
					
					if(level == levelLimit) {
						offset++;

						// level same as before
						setMark(mark);

						return offset;
					}
					break;
				}
				case ',' : {
					mark = offset;
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
							if(offset + getAnonymizeMessageLength() < limit) {
								
								int removedLength = getRemovedLength();
								addAnon(offset, nextOffset);
								limit += getRemovedLength() - removedLength;
								
								if(nextOffset < limit) {
									mark = nextOffset;
								}
							} else {
								// make sure to stop scanning here
								offset = limit;

								break loop;
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
								if(offset + getAnonymizeMessageLength() < limit) {
									
									int removedLength = getRemovedLength();
									addAnon(offset, end);
									limit += getRemovedLength() - removedLength;
									
									if(end < limit) {
										mark = end;
									}
								} else {
									// make sure to stop scanning here
									offset = limit;

									break loop;
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
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']' && chars[nextOffset] > 0x20);

					if(offset + getAnonymizeMessageLength() < limit) {
						
						int removedLength = getRemovedLength();
						addAnon(offset, nextOffset);
						
						limit += getRemovedLength() - removedLength;
						
						if(nextOffset < limit) {
							mark = nextOffset;
						}

					} else {
						// make sure to stop scanning here
						offset = limit;

						break loop;
					}

					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
		}

		setLevel(level);
		setMark(mark);

		return offset;
	}
	
}
