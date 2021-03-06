package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class ByteArrayRangesFilter extends AbstractRangesFilter {
	
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
	static int getChars(int i, int charPos, byte[] buf) {
		i = -i;

		// We know there are at most two digits left at this point.
		int q = i / 10;
		int r = (q * 10) - i;
		buf[--charPos] = (byte)('0' + r);

		// Whatever left is the remaining digit.
		if (q < 0) {
			buf[--charPos] = (byte)('0' - q);
		}

		return charPos;
	}
	protected final byte[] pruneMessage;
	protected final byte[] anonymizeMessage;
	protected final byte[] truncateMessage;
	
	protected final byte[] digit = new byte[11];

	public ByteArrayRangesFilter(int initialCapacity) {
		this(initialCapacity, DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public ByteArrayRangesFilter(int initialCapacity, byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
		super(initialCapacity);
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}


	
	public void filter(final byte[] chars, int offset, int length, final ByteArrayOutputStream buffer) {
		
		// this might be controversial performance-wise; for heavy filtered documents, it might introduce a
		// bottleneck on memory / cache bandwidth
		// alternative approaches would be to keep track of the diff, and thus know exactly 
		// the proper buffer size
		//buffer.ensureCapacity(buffer.length() + length); 
		
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			
			if(filter[i+2] == FILTER_ANON) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.write(chars, offset, filter[i] - offset);
				buffer.write(pruneMessage, 0, pruneMessage.length);
			} else {
				// account for 1-4 bytes UTF-8 encoding
				// i.e. backwards sync
				
				// check the last byte to be excluded
				// if it is not an ascii byte, then we're in the middle of a multi-byte
				// utf-8 character
				
				// byte 1 begins with bits 0xxx xxxx if single char per byte (ascii)
				if( (chars[filter[i] - 1] & 0x80) != 0) {
					// multi-byte character
					
					// check whether the whole character was included, and if not, 
					// exclude it
					
					// rewind to the first byte
					// bytes 2, 3 and 4 all start with bits 10xx xxxx
					// so and'ing with filter 1100 0000 should always result in 
					// bits 1000 0000
					int index = filter[i] - 1;
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
					
					if(index + utfLength == filter[i]) {
						// keep it
					} else {
						// remove it
						int difference = filter[i] - index;
						filter[i] -= difference;
						filter[i+2] -= difference;
					}
				}
				
				// \ u
				// \ uX
				// \ uXX
				// \ uXXX
				//
				// where X is hex
				
				byte peek = chars[filter[i]];
				// check for unicode encoding. That means the peek char must be a hex
				if( (peek >= '0' && peek <= '9') || (peek >= 'A' && peek <= 'F')) {
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
	
	protected final void writeInt(ByteArrayOutputStream out, int v) {
		int chars = getChars(v, 11, digit);
		
		out.write(digit, chars, 11 - chars);
	}
	
	public static int skipObject(byte[] chars, int offset) {
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

	public static int skipSubtree(byte[] chars, int offset) {
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

	public static final int scanBeyondQuotedValue(final byte[] chars, int offset) {
		while(chars[++offset] != '"' || chars[offset - 1] == '\\');

		return offset + 1;
	}

	public static final int scanUnquotedValue(final byte[] chars, int offset) {
		do {
			offset++;
		} while(chars[offset] != ',' && chars[offset] != '}' && chars[offset] != ']');

		return offset;
	}
	
	public static int anonymizeSubtree(byte[] chars, int offset, ByteArrayRangesFilter filter) {
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
