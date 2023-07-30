package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class CharArrayWhitespaceFilter {

	protected static final char[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	protected static final char[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_ANONYMIZE_MESSAGE.toCharArray();
	protected static final char[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;

	protected int start;
	protected byte[] digit = new byte[11];
	
	public CharArrayWhitespaceFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharArrayWhitespaceFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
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

	public int skipObject(final char[] chars, int offset, int limit, final StringBuilder buffer) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {
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
	
	public int anonymizeObjectOrArray(char[] chars, int offset, int limit, StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		// stop processing with level is zero

		int start = getStart();

		while(true) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {		
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

					nextOffset++;
					int postQuoteIndex = nextOffset;
					
					// key or value

					// skip whitespace
					// optimization: scan for highest value
					while(chars[nextOffset] <= 0x20) {
						nextOffset++;
					}

					if(chars[nextOffset] == ':') {
						// was a key
						offset = nextOffset + 1;

						if(nextOffset != postQuoteIndex) {
							buffer.append(chars, start, postQuoteIndex - start);
							buffer.append(':');
							
							start = offset;			
						}
						continue;
					} 
					// was a value
					buffer.append(chars, start, offset - start);
					buffer.append(anonymizeMessage);
					
					if(metrics != null) {
						metrics.onAnonymize(1);
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
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

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
	
	public static int skipWhitespaceFromEnd(char[] chars, int limit) {
		// skip backwards so that we can jump over whitespace without checking limit
		do {
			limit--;
		} while(chars[limit] <= 0x20);
		
		return limit + 1;
	}
	
	public int skipObjectMaxStringLength(final char[] chars, int offset, int limit, int maxStringLength, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
				
				if(nextOffset - offset - 1 > maxStringLength) {
					int endQuoteIndex = nextOffset;
					
					// field name or value, might be whitespace

					// skip whitespace
					// optimization: scan for highest value
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					// TODO avoid flushing if no whitespace? this is a bit unusual place to see a whitespace 
					if(chars[nextOffset] == ':') {
						// was a field name
						buffer.append(chars, start, endQuoteIndex - start + 1);
					} else {
						// was a value
						int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						
						int skipped = endQuoteIndex - aligned;
						
						int remove = skipped - truncateMessage.length - AbstractRangesFilter.lengthToDigits(skipped);
						if(remove > 0) {
							buffer.append(chars, start, aligned - start);
							buffer.append(truncateMessage);
							buffer.append(skipped);
							buffer.append('"');
							
							if(metrics != null) {
								metrics.onMaxStringLength(1);
							}
							
						}
					}

					start = nextOffset;
				}
				offset = nextOffset + 1;

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
	
	public int getPruneMessageLength() {
		return pruneMessage.length;
	}

	public int getAnonymizeMessageLength() {
		return anonymizeMessage.length;
	}
	
	public char[] getAnonymizeMessage() {
		return anonymizeMessage;
	}
	
	public char[] getPruneMessage() {
		return pruneMessage;
	}
	
	public char[] getTruncateMessage() {
		return truncateMessage;
	}
		

}
