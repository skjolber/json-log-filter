package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class CharArrayRangesFilter {
	
	protected static final int MAX_INITIAL_ARRAY_SIZE = 256;
	protected static final int DEFAULT_INITIAL_ARRAY_SIZE = 16;

	public static final int FILTER_PRUNE = 0;
	public static final int FILTER_ANON = 1;
	public static final int FILTER_MAX_LENGTH = 2;
	
	public static final String FILTER_PRUNE_MESSAGE = "SUBTREE REMOVED";
	public static final String FILTER_PRUNE_MESSAGE_JSON = '"' + FILTER_PRUNE_MESSAGE + '"';
	
	public static final String FILTER_ANONYMIZE = "*****";
	public static final String FILTER_ANONYMIZE_MESSAGE = "\"*****\"";
	public static final String FILTER_TRUNCATE_MESSAGE = "...TRUNCATED BY ";

	public static final char[] FILTER_PRUNE_MESSAGE_CHARS = FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	public static final char[] FILTER_ANONYMIZE_MESSAGE_CHARS = FILTER_ANONYMIZE_MESSAGE.toCharArray();
	public static final char[] FILTER_TRUNCATE_MESSAGE_CHARS = FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected int[] filter;
	protected int filterIndex = 0;
	
	public CharArrayRangesFilter(int pathMatches) {
		if(pathMatches == -1) {
			pathMatches = DEFAULT_INITIAL_ARRAY_SIZE;
		}
		this.filter = new int[Math.min(pathMatches, MAX_INITIAL_ARRAY_SIZE) * 3];
	}

	public void addMaxLength(int start, int end, int length) {
		add(start, end, -length);
	}
	
	public void addAnon(int start, int end) {
		add(start, end, FILTER_ANON);
	}
	
	public void addPrune(int start, int end) {
		add(start, end, FILTER_PRUNE);
	}
	
	public void add(int start, int end, int type) {
		if(filter.length <= filterIndex) {
			
			int[] next = new int[filter.length * 2];
			System.arraycopy(filter, 0, next, 0, filter.length);
			
			filter = next;
		}

		filter[filterIndex++] = start;
		filter[filterIndex++] = end;
		filter[filterIndex++] = type;
	}

	public int getFilterIndex() {
		return filterIndex;
	}
	
	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer) {
		
		// this might be controversial performance-wise; for heavy filtered documents, it might introduce a
		// bottleneck on memory / cache bandwidth
		// alternative approaches would be to keep track of the diff, and thus know exactly 
		// the proper buffer size
		buffer.ensureCapacity(buffer.length() + length); 
		
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(FILTER_ANONYMIZE_MESSAGE_CHARS);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(FILTER_PRUNE_MESSAGE_CHARS);
			} else {
				// account for code points and escaping
				
				// A high surrogate precedes a low surrogate.
				if(Character.isHighSurrogate(chars[filter[i] - 1])) {
					filter[i]--;
					filter[i+2]--;
				} else {
					// \ u
					// \ uX
					// \ uXX
					// \ uXXX
					//
					// where X is hex
					
					int index = filter[i] - 1; // index of last character which is included
					
					// absolute minimium is length 8:
					// ["\ uABCD"] (without the space) so
					// ["\ u (without the space) is the minimum
					// so must at least be 2 characters
					
					if(chars[index--] == 'u' && chars[index] == '\\') { // index minimum at 1

						filter[i] -= 2;
						filter[i+2] -= 2;

					} else if(chars[index--] == 'u' && chars[index] == '\\') { // index minimum at 0

						filter[i] -= 3;
						filter[i+2] -= 3;

					} else if(index > 0 && chars[index--] == 'u' && chars[index] == '\\') {

						filter[i] -= 4;
						filter[i+2] -= 4;

					} else if(index > 0 && chars[index--] == 'u' && chars[index] == '\\') {

						filter[i] -= 5;
						filter[i+2] -= 5;

					} else {
						// not unicode encoded
					}
					
					//  \r \n \\ or start of \\uXXXX
					// while loop because we could be looking at an arbitrary number of slashes, i.e.
					// for the usual escaped values, or if one wanted to write an unicode code as text
					while(chars[filter[i] - 1] == '\\') {
						filter[i]--;
						filter[i+2]--;
					}
				}
				
				buffer.append(chars, offset, filter[i] - offset);
				
				
				buffer.append(FILTER_TRUNCATE_MESSAGE_CHARS);
				buffer.append(-filter[i+2]);
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.append(chars, offset, length - offset);
		}
	}
	
	public static int skipObject(char[] chars, int offset) {
		int level = 0;

		while(true) {
			switch(chars[offset]) {
				case '{' : {
					level++;
					break;
				}
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					}
					break;
				}
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				}
				default :
			}
			offset++;
		}
	}	

	public static int skipSubtree(char[] chars, int offset) {
		int level = 0;

		while(true) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					level++;
					break;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					} else if(level < 0) { // was scalar value
						return offset;
					}
					break;
				}
				case ',' : {
					if(level == 0) { // was scalar value
						return offset;
					}
					break;
				}
				case '"' : {
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					
					if(level == 0) {
						return offset + 1;
					}
				}
				default :
			}
			offset++;
		}
	}

	public static final int scanBeyondQuotedValue(final char[] chars, int offset) {
		while(chars[++offset] != '"' || chars[offset - 1] == '\\');

		return offset + 1;
	}

	public static final int scanBeyondUnquotedValue(final char[] chars, int offset) {
		do {
			offset++;
		} while(chars[offset] != ',' && chars[offset] != '}' && chars[offset] != ']');

		return offset;
	}
	
	public static int anonymizeSubtree(char[] chars, int offset, CharArrayRangesFilter filter) {
		int level = 0;

		while(true) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					level++;
					break;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					} else if(level < 0) { // was scalar value
						return offset;
					}
					break;
				}
				case ',' : {
					if(level == 0) { // was scalar value
						return offset;
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
							filter.add(offset, nextOffset, FilterType.ANON.getType());
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
								filter.add(offset, end, FilterType.ANON.getType());
								
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
					
					filter.add(offset, nextOffset, FilterType.ANON.getType());
					
					offset = nextOffset;
					
					continue;
							
				}
			}
			offset++;
		}
	}
	
}
