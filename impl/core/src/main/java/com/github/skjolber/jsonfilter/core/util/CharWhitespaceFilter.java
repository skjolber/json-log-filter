package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class CharWhitespaceFilter {

	protected static final char[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	protected static final char[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_ANONYMIZE_MESSAGE.toCharArray();
	protected static final char[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;

	protected int start;
	protected byte[] digit = new byte[11];
	
	public CharWhitespaceFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharWhitespaceFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
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

	public byte[] getDigit() {
		return digit;
	}

	public char[] getTruncateString() {
		return truncateMessage;
	}

	public int skipObjectOrArray(final char[] chars, int offset, int limit, final StringBuilder buffer) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			if(chars[offset] <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					if(offset >= limit) {
						start = offset;
						break loop;
					}
				} while(chars[offset] <= 0x20);
				
				start = offset;
			}
			
			// 01111011 {
			// 01011011 [
			// 01x11011
			
			// 01011101 ]
			// 01111101 }
			// 01x11101
			
			// 01x11xx1 translates to
			// 01011001 Y (safe to ignore)
			// 01011011 [
			// 01011101 ]
			// 01011111 _ (safe to ignore)
			// 01111001 y (safe to ignore)
			// 01111011 {
			// 01111101 }
			// 01111111 DEL (safe to ignore)
			
			
			// 00100010 "
			// 00101100 ,
			
			if(chars[offset] == '"') {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				offset++;
				
				continue;
			} 
			
			if((chars[offset] & 0b11011001) == 0b01011001) {
				
				/*
				if((chars[offset] & 0x3) == 0x3) {
					level++;
				} else {
					level--;
					if(level == 0) {
						offset++;
						break loop;
					}
				}
*/
				/*
				switch(chars[offset]) {
				case '[':
				case '{': {
					level++;
					break;
				}
				default : {
					level--;
					if(level == 0) {
						offset++;
						break loop;
					}
				}
				}
				*/
				
				
				if(chars[offset] == '[' || chars[offset] == '{') { // alternatively if((chars[offset] & 0x3) == 0x3)
					level++;
				} else {
					level--;
					if(level == 0) {
						offset++;
						break loop;
					}
				}
				
			}
			offset++;
		}
		
		buffer.append(chars, start, offset - start);
		
		setStart(offset);
		
		return offset;
	}
	
	public int skipObjectOrArray2(final char[] chars, int offset, int limit, final StringBuilder buffer) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			if(chars[offset] <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(offset < limit && chars[offset] <= 0x20);
				
				start = offset;

				continue;
			}
			
			switch(chars[offset]) {
			case '"': {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				offset++;
				
				continue;
			}
			case '{' :
			case '[' :
				level++;

				break;
			case '}' :
			case ']' :
				level--;

				if(level == 0) {
					offset++;
					break loop;
				}
				break;
			}
			offset++;
		}
		
		buffer.append(chars, start, offset - start);
		
		setStart(offset);
		
		return offset;
	}
	

	public int skipObject(final char[] chars, int offset, int limit, final StringBuilder buffer) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			if(chars[offset] <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(offset < limit && chars[offset] <= 0x20);
				
				start = offset;

				continue;
			}
			
			switch(chars[offset]) {
			case '"': {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				offset++;
				
				continue;
			}
			case '{' :
				level++;

				break;
			case '}' :
				level--;

				if(level == 0) {
					offset++;
					break loop;
				}
				break;
			}
			offset++;
		}
		
		buffer.append(chars, start, offset - start);
		
		setStart(offset);
		
		return offset;
	}
	
	public boolean anonymizeSubtree3(final char[] chars, int offset, int limit, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int start = getStart();

		while(offset < limit) {
			char c = chars[offset];
			if(c == '"') {
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

				int endQuoteIndex = nextOffset;
				
				// key or value, might be whitespace

				// skip whitespace
				// optimization: scan for highest value
				do {
					nextOffset++;
				} while(chars[nextOffset] <= 0x20);

				if(chars[nextOffset] == ':') {
					// was a key
					buffer.append(chars, start, endQuoteIndex - start + 1);
				} else {
					// was a value
					
				}

				start = nextOffset;

				continue;
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				start = offset;

				continue;
			}
			offset++;
		}
		buffer.append(chars, start, offset - start);
		
		
		
		return false;
	}	
	
	public int anonymizeObjectOrArray(char[] chars, int offset, int limit, StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		int start = getStart();

		while(true) {
			if(chars[offset] <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(offset < limit && chars[offset] <= 0x20);
				
				start = offset;

				continue;
			}
			
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
						setStart(start);
						
						return offset + 1;
					}
					break;
				}
				case ',' : {
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

					int endQuoteIndex = nextOffset;
					
					// key or value

					// skip whitespace
					// optimization: scan for highest value
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] == ':') {
						// was a key
						buffer.append(chars, start, endQuoteIndex - start + 1);
						buffer.append(':');
						
						nextOffset++;
					} else {
						// was a value
						buffer.append(chars, start, offset - start);
						
						buffer.append(anonymizeMessage);
						
						if(metrics != null) {
							metrics.onAnonymize(1);
						}
					}

					offset = nextOffset;
					start = nextOffset;			
					
					continue;
				}
				default : {
					// scalar value
					buffer.append(chars, start, offset - start);

					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']' && chars[nextOffset] > 0x20);
					
					buffer.append(anonymizeMessage);

					if(metrics != null) {
						metrics.onAnonymize(1);
					}

					offset = nextOffset;
					start = nextOffset;
					
					continue;
							
				}
			}
			offset++;
		}
	}
	
	public static int process(char[] chars, int offset, int limit, StringBuilder output) {
		int start = offset;
		
		while(offset < limit) {
			char c = chars[offset];
			if(c == '"') {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				output.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);
				
				start = offset;
				
				continue;
			}
			offset++;
		}
		output.append(chars, start, offset - start);
		
		return offset;
	}
	
	public static int skipWhitespaceBackwards(char[] chars, int limit) {
		// skip backwards so that we can jump over whitespace without checking limit
		do {
			limit--;
		} while(chars[limit] <= 0x20);
		
		return limit + 1;
	}
	
}
