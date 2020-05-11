package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class CharArrayFilter {
	
	protected static final int DELTA_ARRAY_SIZE = 8;
	
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

	private int[] filter;
	private int filterIndex = 0;
	
	public CharArrayFilter() {
		 filter = new int[DELTA_ARRAY_SIZE * 3];
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
			
			int[] next = new int[filter.length + 3 * 4];
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
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			buffer.append(chars, offset, filter[i] - offset);
			
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(FILTER_ANONYMIZE_MESSAGE_CHARS);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(FILTER_PRUNE_MESSAGE_CHARS);
			} else {
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
					offset = scanBeyondQuotedValue(chars, offset);
	
					continue;
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
					offset = scanBeyondQuotedValue(chars, offset);
	
					if(level == 0) {
						return offset;
					}
					
					continue;
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
	
	public static int anonymizeSubtree(char[] chars, int offset, CharArrayFilter filter) {
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
					int nextOffset = scanBeyondQuotedValue(chars, offset);
	
					// is this a field name or a value? A field name must be followed by a colon
					
					// special case: no whitespace
					if(chars[nextOffset] == ':') {
						// key
						offset = nextOffset + 1;
					} else {
						// fast-forward over whitespace
						int end = nextOffset;

						// optimization: scan for highest value
						// space: 0x20
						// tab: 0x09
						// carriage return: 0x0D
						// newline: 0x0A

						while(chars[nextOffset] <= 0x20) {
							nextOffset++;
						}
						if(chars[nextOffset] == ':') {
							// key
							offset = nextOffset + 1;
						} else {
							// value
							filter.add(offset, end, FilterType.ANON.getType());
							
							offset = nextOffset; // +1 since can't be a double quote
						}
					}
					
					continue;
				}
				default : {
					// scalar value
					int nextOffset = CharArrayFilter.scanBeyondUnquotedValue(chars, offset);
					
					filter.add(offset, nextOffset, FilterType.ANON.getType());
					
					offset = nextOffset;
					
					continue;
							
				}
			}
			offset++;
		}
	}
	
}
