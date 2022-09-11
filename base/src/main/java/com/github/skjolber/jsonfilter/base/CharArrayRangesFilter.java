package com.github.skjolber.jsonfilter.base;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class CharArrayRangesFilter extends AbstractRangesFilter {
	
	protected static final char[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	protected static final char[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = FILTER_ANONYMIZE_MESSAGE.toCharArray();
	protected static final char[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;
	
	public CharArrayRangesFilter(int initialCapacity, int length) {
		this(initialCapacity, length, DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharArrayRangesFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		super(initialCapacity, length);
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}

	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(anonymizeMessage);
				
				metrics.onAnonymize(1);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(pruneMessage);
				metrics.onPrune(1);
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.append(chars, offset, filter[i] - offset);
				
				metrics.onMaxSize(length - filter[i]);
			} else {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(truncateMessage);
				buffer.append(-filter[i+2]);
				
				metrics.onMaxStringLength(-filter[i+2]);
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.append(chars, offset, length - offset);
		}
	}

	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer) {
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(anonymizeMessage);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(pruneMessage);
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.append(chars, offset, filter[i] - offset);
			} else {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(truncateMessage);
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
					break;
				}
				default :
			}
			offset++;
		}
	}
	
	public static int skipObjectMaxStringLength(char[] chars, int offset, int maxStringLength, CharArrayRangesFilter filter) {
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
					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
					nextOffset++;
					
					if(nextOffset - offset > maxStringLength) {
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

							if(chars[nextOffset] > 0x20) {
								// was a value
								filter.addMaxLength(chars, offset + maxStringLength - 1, nextOffset - 1, -(offset + maxStringLength - nextOffset));
							} else {
								// fast-forward over whitespace
								// optimization: scan for highest value

								int end = nextOffset;
								do {
									nextOffset++;
								} while(chars[nextOffset] <= 0x20);

								if(chars[nextOffset] == ':') {
									// was a key
									offset = nextOffset + 1;
									
									continue;
								} else {
									// value
									filter.addMaxLength(chars, offset + maxStringLength - 1, end - 1, -(offset + maxStringLength - end));
								}
							}
						}
					}
					offset = nextOffset;
					
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
					do {
						offset++;
					} while(chars[offset] != '"' || chars[offset - 1] == '\\');
					
					if(level == 0) { 
						return offset + 1;
					}
					break;
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

	public static final int scanUnquotedValue(final char[] chars, int offset) {
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
							filter.addAnon(offset, nextOffset);
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
								filter.addAnon(offset, end);
								
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
					
					filter.addAnon(offset, nextOffset);
					
					offset = nextOffset;
					
					continue;
							
				}
			}
			offset++;
		}
	}
	
	public void addMaxLength(char[] chars, int start, int end, int length) {
		// account for code points and escaping
		
		int alignedStart = getStringAlignment(chars, start);
		
		length += start - alignedStart;
		
		super.addMaxLength(alignedStart, end, length);
		
		this.removedLength += end - alignedStart - truncateMessage.length - lengthToDigits(length); // max integer
	}
	
	public void addAnon(int start, int end) {
		super.addAnon(start, end);
		
		this.removedLength += end - start - anonymizeMessage.length;
	}
	
	public void addPrune(int start, int end) {
		super.addPrune(start, end);
		
		this.removedLength += end - start - pruneMessage.length;
	}

	public void addDelete(int start, int end) {
		super.addDelete(start, end);
		
		this.removedLength += end - start;
	}
	
	public int getPruneMessageLength() {
		return pruneMessage.length;
	}

	public int getAnonymizeMessageLength() {
		return anonymizeMessage.length;
	}
	
	
	public static int getStringAlignment(char[] chars, int start) {
		// account for code points and escaping
		
		// A high surrogate precedes a low surrogate. Together they make up a codepoint.
		if(Character.isLowSurrogate(chars[start])) {
			start--;
		} else {
			// \ u
			// \ uX
			// \ uXX
			// \ uXXX
			//
			// where X is hex

			char peek = chars[start];
			// check for unicode encoding. That means the peek char must be a hex
			if( (peek >= '0' && peek <= '9') || (peek >= 'A' && peek <= 'F')) {
				int index = start - 1; // index of last character which is included
				
				// absolute minimum is length 8:
				// ["\ uABCD"] (without the space) so
				// ["\ u (without the space) is the minimum
				// so must at least be 2 characters
				
				int offset;
				if(chars[index--] == 'u' && chars[index] == '\\') { // index minimum at 1
					offset = 2;
				} else if(chars[index--] == 'u' && chars[index] == '\\') { // index minimum at 0
					offset = 3;
				} else if(index > 0 && chars[index--] == 'u' && chars[index] == '\\') {
					offset = 4;
				} else if(index > 0 && chars[index--] == 'u' && chars[index] == '\\') {
					offset = 5;
				} else {
					// not unicode encoded
					offset = 0;
				}
				if(offset > 0) {
					int slashCount = 1;
					// is the unicode encoded with an odd number of slashes?
					while(chars[index - slashCount] == '\\') {
						slashCount++;
					}
					if(slashCount % 2 == 1) {
						start -= offset;
					}
				}
			} else {
				// not unicode encoded
				
				//  \r \n \\ or start of \\uXXXX
				// while loop because we could be looking at an arbitrary number of slashes, i.e.
				// for the usual escaped values, or if one wanted to write an unicode code as text
				if(chars[start - 1] == '\\') {
					int slashCount = 2;
					// is the unicode encoded with an odd number of slashes?
					while(chars[start - slashCount] == '\\') {
						slashCount++;
					}
					if(slashCount % 2 == 0) {
						start--;
					}
				}
			}
		}
		return start;
	}


}
