package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteWhitespaceFilter {

	protected static final byte[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_PRUNE_MESSAGE_JSON.getBytes(StandardCharsets.UTF_8);
	protected static final byte[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_ANONYMIZE_MESSAGE.getBytes(StandardCharsets.UTF_8);
	protected static final byte[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = AbstractRangesFilter.FILTER_TRUNCATE_MESSAGE.getBytes(StandardCharsets.UTF_8);

	protected final byte[] pruneMessage;
	protected final byte[] anonymizeMessage;
	protected final byte[] truncateMessage;

	protected int start;
	protected int mark;
	protected int writtenMark;
	protected byte[] digit = new byte[11];
	
	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	

	public ByteWhitespaceFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public ByteWhitespaceFilter(byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
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
	
	public int getWrittenMark() {
		return writtenMark;
	}

	public void setWrittenMark(int writtenMark) {
		this.writtenMark = writtenMark;
	}
	
	public boolean[] grow(boolean[] squareBrackets) {
		boolean[] next = new boolean[squareBrackets.length + 32];
		System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
		this.squareBrackets = next;
		return next;
	}

	public boolean[] getSquareBrackets() {
		return squareBrackets;
	}
	
	public int getLevel() {
		return level;
	}
	
	public int getMark() {
		return mark;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setMark(int mark) {
		this.mark = mark;
	}

	public void closeStructure(final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
	}

	public int markToLimit(char[] chars) {
		switch(chars[mark]) {
			
			case '{' :
			case '}' :
			case '[' :
			case ']' :
				return mark + 1;
			default : {
				return mark;
			}
		}
	}

	public byte[] getDigit() {
		return digit;
	}

	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public static void process(byte[] chars, int offset, int limit, ByteArrayOutputStream output) {
		int start = offset;
		
		while(offset < limit) {
			byte c = chars[offset];
			if(c == '"') {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
			} else if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, start, offset - start);
				do {
					offset++;
				} while(chars[offset] <= 0x20);
				
				start = offset;
				
				continue;
			}
			offset++;
		}
		output.write(chars, start, offset - start);
	}
	
	public int skipObject(final byte[] chars, int offset, int limit, final ByteArrayOutputStream buffer) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
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
		
		buffer.write(chars, start, offset - start);
		
		setStart(offset);
		
		return offset;
	}

	public int anonymizeObjectOrArray(byte[] chars, int offset, int limit, ByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		int level = 1;

		int start = getStart();

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

	public int skipObjectMaxStringLength(byte[] chars, int offset, int limit, int maxStringLength, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		int level = 1;

		int start = getStart();

		loop: while(offset < limit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				// skip this char and any other whitespace
				output.write(chars, start, offset - start);
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
						output.write(chars, start, endQuoteIndex - start + 1);
					} else {
						// was a value
						int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						output.write(chars, start, aligned - start);
						output.write(truncateMessage, 0, truncateMessage.length);
						ByteArrayRangesFilter.writeInt(output, endQuoteIndex - aligned, digit);
						output.write('"');

						if(metrics != null) {
							metrics.onMaxStringLength(1);
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
		
		output.write(chars, start, offset - start);
		
		setStart(offset);
		
		return offset;

	}

}
