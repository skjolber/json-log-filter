package com.github.skjolber.jsonfilter.core.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteArrayWhitespaceFilter {

	// Word-at-a-time whitespace detection: reads 8 bytes as a long (little-endian).
	// Uses the Hacker's Delight has-zero-byte trick to detect bytes < 0x21 (i.e., <= 0x20 = whitespace).
	private static final VarHandle LONG_LE =
		MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	private static final long MAGIC2 = 0x8080808080808080L;
	// For detecting bytes > 0x20 (non-whitespace) in a word: hasGreaterThan(word, 0x20).
	// Formula: (((word & ~MAGIC2) + WS_GT_MASK) | word) & MAGIC2 != 0  iff any byte > 0x20.
	// WS_GT_MASK = (0x7F - 0x20) * 0x0101010101010101L = 0x5F * MAGIC1
	private static final long WS_GT_MASK = 0x5F5F5F5F5F5F5F5FL;

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
		final int safeEnd = limit - 8;

		while(offset < limit) {
			byte c = chars[offset];
			// Fast path: structural JSON bytes (`:`, `,`, `{`, `}`, `[`, `]`, digits, keywords)
			// are all > 0x22 ('"'). Check this first to avoid the quote and whitespace tests
			// on the most common non-string bytes.
			if(c > '"') {
				offset++;
				continue;
			}
			if(c == '"') {
				offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
				continue;
			}
			// c <= 0x21: whitespace (c <= 0x20) — skip and flush
			// (c == 0x21 '!' cannot appear as structural JSON, treated safely as whitespace)
			output.write(chars, flushOffset, offset - flushOffset);
			// Word-at-a-time whitespace skip for long runs (pretty-printed JSON)
			offset++;
			while(offset <= safeEnd) {
				long word = (long) LONG_LE.get(chars, offset);
				// Continue only while ALL 8 bytes are whitespace (none > 0x20).
				long hasNonWs = (((word & ~MAGIC2) + WS_GT_MASK) | word) & MAGIC2;
				if(hasNonWs != 0) {
					break;
				}
				offset += 8;
			}
			while(chars[offset] <= 0x20) {
				offset++;
			}
			flushOffset = offset;
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
				
				// key or value, might be followed by whitespace
				nextOffset++;
				
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						if(chars[nextOffset] == ':') {
							// whitespace before colon
							output.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
							output.write(':');
							
							nextOffset++;

							if(chars[nextOffset] <= 0x20) {
								// whitespace before and after colon
								do {
									nextOffset++;
								} while(chars[nextOffset] <= 0x20);				
							} else {
								// whitespace before colon, but not after
							}

							flushOffset = nextOffset;
							offset = nextOffset;
							continue;
						}
					} 
					
					// was a value
					if(endQuoteIndex - offset >= maxStringLength) {
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
					
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);				
					
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

	public static int addMaxLength(final byte[] chars, int offset, final ResizableByteArrayOutputStream output, int flushOffset, int endQuoteIndex, byte[] truncateStringValueAsBytes, int maxStringLength, byte[] digit, JsonFilterMetrics metrics) {
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
