package com.github.skjolber.jsonfilter.core.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteArrayRangesFilter extends AbstractRangesFilter {

	// Word-at-a-time quote scanning: reads 8 bytes as a long (little-endian) and
	// uses the Hacker's Delight has-zero-byte trick to detect '"' (0x22) in bulk.
	private static final VarHandle LONG_LE =
		MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	private static final long QUOTE_MASK     = 0x2222222222222222L; // '"' (0x22) repeated 8×
	private static final long OPEN_OBJ_MASK  = 0x7B7B7B7B7B7B7B7BL; // '{' (0x7B) repeated 8×
	private static final long CLOSE_OBJ_MASK = 0x7D7D7D7D7D7D7D7DL; // '}' (0x7D) repeated 8×
	private static final long OPEN_ARR_MASK  = 0x5B5B5B5B5B5B5B5BL; // '[' (0x5B) repeated 8×
	private static final long CLOSE_ARR_MASK = 0x5D5D5D5D5D5D5D5DL; // ']' (0x5D) repeated 8×
	private static final long MAGIC1         = 0x0101010101010101L;
	private static final long MAGIC2         = 0x8080808080808080L;

	protected static final byte[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = FILTER_PRUNE_MESSAGE_JSON.getBytes(StandardCharsets.UTF_8);
	protected static final byte[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = FILTER_ANONYMIZE_MESSAGE.getBytes(StandardCharsets.UTF_8);
	protected static final byte[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = FILTER_TRUNCATE_MESSAGE.getBytes(StandardCharsets.UTF_8);

	protected static final byte[] DigitTens = {
		'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
		'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
		'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
		'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
		'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
		'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
		'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
		'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
		'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
		'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
		} ;

	protected static final byte[] DigitOnes = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		};

	/**
	 * Places characters representing the integer i into the
	 * character array buf. The characters are placed into
	 * the buffer backwards starting with the least significant
	 * digit at the specified index (exclusive), and working
	 * backwards from there.
	 *
	 * @implNote This method converts positive inputs into negative
	 * values, to cover the Integer.MIN_VALUE case. Converting otherwise
	 * (negative to positive) will expose -Integer.MIN_VALUE that overflows
	 * integer.
	 *
	 * @param i	 value to convert
	 * @param index next index, after the least significant digit
	 * @param buf   target buffer, Latin1-encoded
	 * @return index of the most significant digit or minus sign, if present
	 */
    static int getChars(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;

        boolean negative = i < 0;
        if (!negative) {
            i = -i;
        }

        // Generate two digits per iteration
        while (i <= -100) {
            q = i / 100;
            r = (q * 100) - i;
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // We know there are at most two digits left at this point.
        q = i / 10;
        r = (q * 10) - i;
        buf[--charPos] = (byte)('0' + r);

        // Whatever left is the remaining digit.
        if (q < 0) {
            buf[--charPos] = (byte)('0' - q);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }
	
	protected final byte[] pruneMessage;
	protected final byte[] anonymizeMessage;
	protected final byte[] truncateMessage;
	
	protected final byte[] digit = new byte[11];

	public ByteArrayRangesFilter(int initialCapacity, int length) {
		this(initialCapacity, length, DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public ByteArrayRangesFilter(int initialCapacity, int length, byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
		super(initialCapacity, length);
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}

	public void filter(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		
		if(metrics != null) {
			metrics.onInput(length);
		}

		length += offset;
		
		int bufferSize = buffer.size();

		for(int i = 0; i < filterIndex; i += 3) {
			
			if(filter[i+2] == FILTER_ANON) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(pruneMessage, 0, pruneMessage.length);
				
				if(metrics != null) {
					metrics.onPrune(1);
				}
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.write(chars, offset, filter[i] - offset);
				if(metrics != null) {
					metrics.onMaxSize(length - filter[i]);
				}
			} else {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(truncateMessage, 0, truncateMessage.length);
				writeInt(buffer, -filter[i+2]);
				
				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.write(chars, offset, length - offset);
		}
		
		if(metrics != null) {
			metrics.onOutput(buffer.size() - bufferSize);
		}
	}

	public void filter(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream buffer) {
		length += offset;
		
		int filterIndex = this.filterIndex;
		int[] filter = this.filter;
		
		for(int i = 0; i < filterIndex; i += 3) {
			
			if(filter[i+2] == FILTER_ANON) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(pruneMessage, 0, pruneMessage.length);
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.write(chars, offset, filter[i] - offset);
			} else {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(truncateMessage, 0, truncateMessage.length);
				writeInt(buffer, -filter[i+2]);
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.write(chars, offset, length - offset);
		}
	}
	
	public boolean addMaxLength(byte[] chars, int start, int end, int length) {
		// account for code points and escaping
		if(length < 0) {
			throw new IllegalArgumentException("Negative length " + length);
		}
		
		int alignedStart = getStringAlignment(chars, start);
		
		length += start - alignedStart;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = end - alignedStart - truncateMessage.length - lengthToDigits(length); // max integer
		if(remove > 0) {
			super.addMaxLength(alignedStart, end, length);
			
			this.removedLength += remove;
			
			return true;
		}
		return false;
	}

	
	
	public static int getStringAlignment(byte[] chars, int start) {

		// account for 1-4 bytes UTF-8 encoding
		// i.e. backwards sync
		
		// check the last byte to be excluded
		// if it is not an ascii byte, then we're in the middle of a multi-byte
		// utf-8 character
		
		// byte 1 begins with bits 0xxx xxxx if single char per byte (ascii)
		if( (chars[start - 1] & 0x80) != 0) {
			// multi-byte character
			
			// check whether the whole character was included, and if not, 
			// exclude it
			
			// rewind to the first byte
			// bytes 2, 3 and 4 all start with bits 10xx xxxx
			// so and'ing with filter 1100 0000 should always result in 
			// bits 1000 0000
			int index = start - 1;
			while( (chars[index] & 0xC0) == 0x80) { 
				index--;
			}

			//	  8421
			// 2: 110x xxxx
			// 3: 1110 xxxx
			// 4: 1111 0xxx
			
			int utfLength;
			if((chars[index] & 0xF0) == 0xF0) { // 4
				utfLength = 4;
			} else if((chars[index] & 0xE0) == 0xE0) { // 3
				utfLength = 3;
			} else { // 2
				utfLength = 2;
			}
			
			if(index + utfLength == start) {
				// keep it
			} else {
				// remove it
				int difference = start - index;
				start -= difference;
			}
		}
		
		// \ u
		// \ uX
		// \ uXX
		// \ uXXX
		//
		// where X is hex
		
		byte peek = chars[start];
		// check for unicode encoding. That means the peek char must be a hex
		if( (peek >= '0' && peek <= '9') || (peek >= 'A' && peek <= 'F')) {
			int index = start - 1; // index of last character which is included
			
			// absolute minimium is length 8:
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
		
		return start;
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
	
	public final void writeInt(ResizableByteArrayOutputStream out, int v) {
		writeInt(out, v, digit);
	}
	
	public static final void writeInt(ResizableByteArrayOutputStream out, int v, byte[] digit) {
		int chars = getChars(v, 11, digit);
		
		out.write(digit, chars, 11 - chars);
	}
	
	public static int skipObject(byte[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
				case '{' : {
					level++;
					continue;
				}
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					}
					continue;
				}
				case '"' :
					offset = scanQuotedValue(chars, offset);
					continue;
				case 't': 
				case 'n': {
					offset += 3;
					continue;
				}
				case 'f': {
					offset += 4;
					continue;
				}					
				default :
			}
		}
	}
	
	public static int skipArray(byte[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
				case '[' : {
					level++;
					continue;
				}
				case ']' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					}
					continue;
				}
				case '"' :
					offset = scanQuotedValue(chars, offset);
					continue;
				case 't': 
				case 'n': {
					offset += 3;
					continue;
				}
				case 'f': {
					offset += 4;
					continue;
				}					
				default :
			}
		}
	}
	
	public static final int scanBeyondQuotedValue(final byte[] chars, int offset) {
		return scanQuotedValue(chars, offset) + 1;
	}

	/**
	 * Scan a quoted JSON string value, returning the index of the closing '"'.
	 *
	 * <p>Uses a 16-byte scalar preamble followed by a dual-VarHandle inner loop:
	 * <ul>
	 *   <li>Scalar preamble (first 16 bytes): simple ldrb+cmp per byte, runs at
	 *       ~1 cycle/byte on ARM64. Handles short strings (≤ 15 bytes) without
	 *       VarHandle setup cost — typical for compact JSON field names and IDs.
	 *   <li>Dual-VarHandle loop (bytes 16+): two consecutive {@code getLong} loads
	 *       per iteration (16 bytes/iter) using the Hacker's Delight has-zero-byte
	 *       trick; see <em>Hacker's Delight</em>, 2nd ed., §6-1 "Find First 0-Byte".
	 *       Early-exit on the first load avoids reading the second when a quote is
	 *       found quickly (common in dense JSON).
	 *   <li>8-byte single-load tail and scalar tail for the final < 16 bytes.
	 * </ul>
	 *
	 * <p>The scalar preamble break-even: on ARM64 (Apple Silicon, JDK 25), scalar
	 * byte-by-byte comparison executes at near 1 cycle/byte with correct branch
	 * prediction, while one VarHandle load + has-zero-byte arithmetic costs ~6–8
	 * cycles for 8 bytes. For strings ≤ 15 bytes scalar is therefore faster or
	 * equal; for strings ≥ 16 bytes the preamble replaces the first VarHandle
	 * iteration at equivalent cost, keeping long-string throughput unchanged.
	 */
	public static final int scanQuotedValue(final byte[] chars, int offset) {
		int i = offset + 1;
		// Scalar preamble: scan the first 16 bytes char-by-char.
		// Short strings (≤ 15 bytes, e.g. JSON keys, IDs, enum values) finish
		// here without incurring VarHandle overhead.
		final int preambleEnd = Math.min(i + 16, chars.length);
		while (i < preambleEnd) {
			if (chars[i] == '"') {
				if (chars[i - 1] != '\\') return i;
				return scanEscapedValue(chars, i);
			}
			i++;
		}
		// Process 16 bytes per iteration (two consecutive VarHandle loads)
		final int safeEnd16 = chars.length - 16;
		while (i <= safeEnd16) {
			long word1 = (long) LONG_LE.get(chars, i);
			long x1 = word1 ^ QUOTE_MASK;
			long y1 = (x1 - MAGIC1) & ~x1 & MAGIC2;
			if (y1 != 0) {
				i += Long.numberOfTrailingZeros(y1) >>> 3;
				if (chars[i - 1] != '\\') return i;
				return scanEscapedValue(chars, i);
			}
			long word2 = (long) LONG_LE.get(chars, i + 8);
			long x2 = word2 ^ QUOTE_MASK;
			long y2 = (x2 - MAGIC1) & ~x2 & MAGIC2;
			if (y2 != 0) {
				i += 8 + (Long.numberOfTrailingZeros(y2) >>> 3);
				if (chars[i - 1] != '\\') return i;
				return scanEscapedValue(chars, i);
			}
			i += 16;
		}

		// 8-byte loop for 8–15 byte tail
		final int safeEnd8 = chars.length - 8;
		while (i <= safeEnd8) {
			long word = (long) LONG_LE.get(chars, i);
			long x = word ^ QUOTE_MASK;
			long y = (x - MAGIC1) & ~x & MAGIC2;
			if (y != 0) {
				i += Long.numberOfTrailingZeros(y) >>> 3;
				if (chars[i - 1] != '\\') return i;
				return scanEscapedValue(chars, i);
			}
			i += 8;
		}

		// Scalar tail for remaining bytes (< 8)
		while (chars[i] != '"') i++;
		if (chars[i - 1] != '\\') {
			return i;
		}
		return scanEscapedValue(chars, i);
	}

	public static int scanEscapedValue(final byte[] chars, int offset) {
		while(true) {
			// is there an even number of slashes behind the last '"'?
			int slashOffset = offset - 2;
			while(chars[slashOffset] == '\\') {
				slashOffset--;
			}
			if((offset - slashOffset) % 2 == 1) {
				return offset;
			}
			// Advance past the escaped quote using 16-byte word-at-a-time scan
			int i = offset + 1;
			final int safeEnd16 = chars.length - 16;
			while (i <= safeEnd16) {
				long word1 = (long) LONG_LE.get(chars, i);
				long x1 = word1 ^ QUOTE_MASK;
				long y1 = (x1 - MAGIC1) & ~x1 & MAGIC2;
				if (y1 != 0) {
					i += Long.numberOfTrailingZeros(y1) >>> 3;
					break;
				}
				long word2 = (long) LONG_LE.get(chars, i + 8);
				long x2 = word2 ^ QUOTE_MASK;
				long y2 = (x2 - MAGIC1) & ~x2 & MAGIC2;
				if (y2 != 0) {
					i += 8 + (Long.numberOfTrailingZeros(y2) >>> 3);
					break;
				}
				i += 16;
			}
			final int safeEnd8 = chars.length - 8;
			while (i <= safeEnd8) {
				long word = (long) LONG_LE.get(chars, i);
				long x = word ^ QUOTE_MASK;
				long y = (x - MAGIC1) & ~x & MAGIC2;
				if (y != 0) {
					i += Long.numberOfTrailingZeros(y) >>> 3;
					break;
				}
				i += 8;
			}
			while (chars[i] != '"') i++;
			offset = i;
			if (chars[offset - 1] != '\\') {
				return offset;
			}
		}
	}

	public static final int scanBeyondUnquotedValue(final byte[] chars, int offset) {
		while(true) {
			switch(chars[++offset]) {
			case ',':
			case '}':
			case ']': 
			case ' ': 
			case '\r': 
			case '\t': 
			case '\n': 
				return offset;
				default:
			}
		}
	}

	public static int skipObjectOrArray(byte[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
				case '[' : 
				case '{' : {
					level++;
					continue;
				}
	
				case ']' : 
				case '}' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					}
					continue;
				}
				case '"' :
					offset = scanQuotedValue(chars, offset);
					continue;
				case 't': 
				case 'n': {
					offset += 3;
					continue;
				}
				case 'f': {
					offset += 4;
					continue;
				}					
			}
		}
	}

	public static int anonymizeObjectOrArray(byte[] chars, int offset, ByteArrayRangesFilter filter) {
		offset++;
		
		int level = 1;

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
					int nextOffset = scanBeyondQuotedValue(chars, offset);
	
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
					} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']' && chars[nextOffset] > 0x20);
					
					filter.addAnon(offset, nextOffset);
					
					offset = nextOffset;
					
					continue;
							
				}
			}
			offset++;
		}
	}
	
	
	public static int skipObjectMaxStringLength(byte[] chars, int offset, int maxStringLength, ByteArrayRangesFilter filter) {
		int level = 1;

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
					int nextOffset = scanBeyondQuotedValue(chars, offset);
					
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
				case 't': 
				case 'n': {
					offset += 4;
					continue;
				}
				case 'f': {
					offset += 5;
					continue;
				}					
				default :
			}
			offset++;
		}
	}

	public int getAnonymizeMessageLength() {
		return anonymizeMessage.length;
	}

	public int getPruneMessageLength() {
		return pruneMessage.length;
	}
}
