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
	
	/**
	 * Skip a run of whitespace.
	 *
	 * @param chars input
	 * @param offset index of a whitespace byte
	 * @return index of the first byte after offset which is not whitespace
	 */

	public static int skipWhitespace(final byte[] chars, int offset) {
		// unrolled: one loop test and safepoint poll per four bytes, as there is no limit to count to
		while(true) {
			if(chars[offset + 1] > 0x20) {
				return offset + 1;
			}
			if(chars[offset + 2] > 0x20) {
				return offset + 2;
			}
			if(chars[offset + 3] > 0x20) {
				return offset + 3;
			}
			if(chars[offset + 4] > 0x20) {
				return offset + 4;
			}
			offset += 4;
		}
	}

	public static void process(byte[] chars, int offset, int limit, ResizableByteArrayOutputStream output) {
		// whitespace is only removed, so the output is no longer than the input: make room once,
		// and copy into the buffer directly, so that the loop does not use the output stream
		int written = output.size();
		output.ensureCapacity(written + limit - offset);
		byte[] buffer = output.getBuffer();

		int flushOffset = offset;
		
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
				continue;
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				System.arraycopy(chars, flushOffset, buffer, written, offset - flushOffset);
				written += offset - flushOffset;
				offset = skipWhitespace(chars, offset);
				
				flushOffset = offset;
				
				continue;
			}
			offset++;
		}
		System.arraycopy(chars, flushOffset, buffer, written, offset - flushOffset);
		output.setSize(written + offset - flushOffset);
	}

	/**
	 * Find the first whitespace outside of strings, so that a document without any can be returned as it is.
	 *
	 * @param chars input
	 * @param offset start offset
	 * @param limit end offset
	 * @return index of the first whitespace byte, or limit if there is none
	 */

	public static int findWhitespace(byte[] chars, int offset, int limit) {
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
			} else if(c <= 0x20) {
				return offset;
			} else {
				offset++;
			}
		}
		return limit;
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
				offset = skipWhitespace(chars, offset);

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
				offset = skipWhitespace(chars, offset);

				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int endQuoteIndex = nextOffset;
				
				// key or value, might be followed by whitespace
				nextOffset++;
				
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						nextOffset = skipWhitespace(chars, nextOffset);

						if(chars[nextOffset] == ':') {
							// whitespace before colon
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
							output.write(':');
							
							nextOffset++;

							if(chars[nextOffset] <= 0x20) {
								// whitespace before and after colon
								nextOffset = skipWhitespace(chars, nextOffset);
							} else {
								// whitespace before colon, but not after
							}

							flushOffset = nextOffset;
							offset = nextOffset;
							continue;
						}

						if(endQuoteIndex - offset < maxStringLength + 2) {
							// was a value, followed by whitespace
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);

							flushOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} 
					
					// was a value
					if(endQuoteIndex - offset >= maxStringLength + 2) {
						ByteArrayWhitespaceFilter.addMaxLength(chars, offset, output, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, digit, metrics);
						
						flushOffset = nextOffset;
					}
						
					offset = nextOffset;
					
					continue;
				} else {
					// was a key
					nextOffset++;

					if(chars[nextOffset] > 0x20) {
						// no whitespace before or after colon
						
						offset = nextOffset;
						continue;
					}

					// whitespace after colon, but not before
					output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					output.write(':');
					
					nextOffset = skipWhitespace(chars, nextOffset);
					
					flushOffset = nextOffset;
					offset = nextOffset;
				}
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

	public static int addMaxLength(final byte[] chars, int offset, final ResizableByteArrayOutputStream output, int flushOffset, int endQuoteIndex, byte[] truncateStringValueAsBytes,
			int maxStringLength, byte[] digit, JsonFilterMetrics metrics) {
		// was a value
		int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
		
		int removed = endQuoteIndex - aligned;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = removed - truncateStringValueAsBytes.length - AbstractRangesFilter.lengthToDigits(removed);
		if(remove > 0) {
			output.write(chars, flushOffset, aligned - flushOffset);
			output.write(truncateStringValueAsBytes, 0, truncateStringValueAsBytes.length);
			ByteArrayRangesFilter.writeInt(output, removed, digit);
			output.write('"');
			
			if(metrics != null) {
				metrics.onMaxStringLength(1);
			}
			return remove;
		} else {
			output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
			
			return 0;
		}
	}
	
	public byte[] getDigit() {
		return digit;
	}
	
	public byte[] getAnonymizeMessage() {
		return anonymizeMessage;
	}
	
	public byte[] getPruneMessage() {
		return pruneMessage;
	}
	
	public byte[] getTruncateMessage() {
		return truncateMessage;
	}
}
