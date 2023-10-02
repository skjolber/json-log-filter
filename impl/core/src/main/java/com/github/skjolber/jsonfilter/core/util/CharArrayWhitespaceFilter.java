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

	protected int flushOffset;
	protected byte[] digit = new byte[11];
	
	public CharArrayWhitespaceFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharArrayWhitespaceFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}
	
	public void setFlushOffset(int start) {
		this.flushOffset = start;
	}
	
	public int getFlushOffset() {
		return flushOffset;
	}

	public int anonymizeObjectOrArray(char[] chars, int offset, int limit, StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		// stop processing with level is zero

		int flushOffset = getFlushOffset();

		while(true) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				flushOffset = offset;
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
						setFlushOffset(flushOffset);
						
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
					int nextOffset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
					
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
							buffer.append(chars, flushOffset, postQuoteIndex - flushOffset);
							buffer.append(':');
							
							flushOffset = offset;			
						}
						continue;
					} 
					// was a value
					buffer.append(chars, flushOffset, offset - flushOffset);
					buffer.append(anonymizeMessage);
					
					if(metrics != null) {
						metrics.onAnonymize(1);
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;
					
					continue;
				}
				default : {
					// scalar value
					buffer.append(chars, flushOffset, offset - flushOffset);

					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

					buffer.append(anonymizeMessage);

					if(metrics != null) {
						metrics.onAnonymize(1);
					}

					offset = nextOffset;
					flushOffset = nextOffset;
					
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
				offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
				continue;
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
	
	public int skipObjectMaxStringLength(final char[] chars, int offset, int maxStringLength, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		int flushOffset = getFlushOffset();

		loop: while(true) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int endQuoteIndex = nextOffset;
				
				// key or value, might be whitespace
				nextOffset++;
				
				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					// was a value

					if(endQuoteIndex - offset >= maxStringLength) {
						CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, metrics);
					} else {
						buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);			
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;
					
					continue;
				}

				// was a key
				buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.append(':');

				do {
					nextOffset++;
				} while(chars[nextOffset] <= 0x20);				
				
				offset = nextOffset;
				flushOffset = nextOffset;
				
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
		
		buffer.append(chars, flushOffset, offset - flushOffset);
		
		setFlushOffset(offset);
		
		return offset;
	}
	
	public int skipArrayMaxStringLength(final char[] chars, int offset, int maxStringLength, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int level = 1;

		int flushOffset = getFlushOffset();

		loop: while(true) {
			char c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int endQuoteIndex = nextOffset;
				
				// key or value, might be whitespace
				nextOffset++;
				
				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					// was a value

					if(endQuoteIndex - offset >= maxStringLength) {
						CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, metrics);
					} else {
						buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);			
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;
					
					continue;
				}

				// was a key
				buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.append(':');

				do {
					nextOffset++;
				} while(chars[nextOffset] <= 0x20);				
				
				offset = nextOffset;
				flushOffset = nextOffset;
				
				continue;
			}
			case '[' :
				level++;

				break;
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
		
		buffer.append(chars, flushOffset, offset - flushOffset);
		
		setFlushOffset(offset);
		
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
		
	public static int addMaxLength(final char[] chars, int offset, final StringBuilder buffer, int flushOffset, int endQuoteIndex, char[] truncateStringValue, int maxStringLength, JsonFilterMetrics metrics) {
		// was a value
		int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
		
		int removed = endQuoteIndex - aligned;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = removed - truncateStringValue.length - AbstractRangesFilter.lengthToDigits(removed);
		if(remove > 0) {
			buffer.append(chars, flushOffset, aligned - flushOffset);
			buffer.append(truncateStringValue);
			buffer.append(endQuoteIndex - aligned);
			buffer.append('"');
			
			if(metrics != null) {
				metrics.onMaxStringLength(1);
			}
			return remove;
		} else {
			buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
			
			return 0;
		}
	}

}
