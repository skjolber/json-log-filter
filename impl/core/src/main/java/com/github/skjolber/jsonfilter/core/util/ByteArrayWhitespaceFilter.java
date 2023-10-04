package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteArrayWhitespaceFilter {

	protected final byte[] pruneMessage;
	protected final byte[] anonymizeMessage;
	protected final byte[] truncateMessage;

	protected int flushOffset;
	protected byte[] digit = new byte[11];

	public ByteArrayWhitespaceFilter(byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
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
	
	public static void process(byte[] chars, int offset, int limit, ResizableByteArrayOutputStream output) {
		int flushOffset = offset;
		
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
				continue;
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);
				
				flushOffset = offset;
				
				continue;
			}
			offset++;
		}
		output.write(chars, flushOffset, offset - flushOffset);
	}
	
	public int anonymizeObjectOrArray(byte[] chars, int offset, int limit, ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		int level = 1;

		// stop processing with level is zero
		
		int start = getFlushOffset();

		while(true) {
			byte c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				buffer.write(chars, start, offset - start);
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
						setFlushOffset(start);
						
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
					int nextOffset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
					
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
							buffer.write(chars, start, postQuoteIndex - start);
							buffer.write(':');
							
							start = offset;			
						}
						continue;
					} 
					// was a value
					buffer.write(chars, start, offset - start);
					buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
					
					if(metrics != null) {
						metrics.onAnonymize(1);
					}
					offset = nextOffset;
					start = nextOffset;							
					
					continue;
				}
				default : {
					// scalar value
					buffer.write(chars, start, offset - start);

					int nextOffset = offset;
					do {
						nextOffset++;
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

					buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
					
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

	public static int skipWhitespaceFromEnd(byte[] chars, int limit) {
		// skip backwards so that we can jump over whitespace without checking limit
		do {
			limit--;
		} while(chars[limit] <= 0x20);
		
		return limit + 1;
	}

	public int skipObjectMaxStringLength(byte[] chars, int offset, int maxStringLength, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int level = 1;

		int flushOffset = getFlushOffset();

		loop: while(true) {
			byte c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				
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
						ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, digit, metrics);
					} else {
						output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);			
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;
					
					continue;
				}

				// was a key
				output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				output.write(':');

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
		
		output.write(chars, flushOffset, offset - flushOffset);
		
		setFlushOffset(offset);
		
		return offset;

	}

	public int skipArrayMaxStringLength(byte[] chars, int offset, int maxStringLength, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int level = 1;

		int flushOffset = getFlushOffset();

		loop: while(true) {
			byte c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
				} while(chars[offset] <= 0x20);

				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				
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
						ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, digit, metrics);
					} else {
						output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);			
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;
					
					continue;
				}

				// was a key
				output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				output.write(':');

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
		
		output.write(chars, flushOffset, offset - flushOffset);
		
		setFlushOffset(offset);
		
		return offset;

	}
	
	public static int addMaxLength(final byte[] chars, int offset, final ResizableByteArrayOutputStream output, int start, int endQuoteIndex, byte[] truncateStringValueAsBytes, int maxStringLength, byte[] digit, JsonFilterMetrics metrics) {
		// was a value
		int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
		
		int removed = endQuoteIndex - aligned;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = removed - truncateStringValueAsBytes.length - AbstractRangesFilter.lengthToDigits(removed);
		if(remove > 0) {
			output.write(chars, start, aligned - start);
			output.write(truncateStringValueAsBytes, 0, truncateStringValueAsBytes.length);
			ByteArrayRangesFilter.writeInt(output, removed, digit);
			output.write('"');
			
			if(metrics != null) {
				metrics.onMaxStringLength(1);
			}
			return remove;
		} else {
			output.write(chars, start, endQuoteIndex - start + 1);
			
			return 0;
		}
	}
	
	public byte[] getDigit() {
		return digit;
	}
}
