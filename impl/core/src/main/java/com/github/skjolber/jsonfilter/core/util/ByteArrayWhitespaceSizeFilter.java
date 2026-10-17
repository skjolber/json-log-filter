package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class ByteArrayWhitespaceSizeFilter extends ByteArrayWhitespaceFilter {

	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	
	
	protected int mark;
	protected int writtenMark;

	public ByteArrayWhitespaceSizeFilter(byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
		super(pruneMessage, anonymizeMessage, truncateMessage);
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

	public void setMaxSizeLimit(int limit) {
		this.limit = limit;
	}
	
	public int getMaxSizeLimit() {
		return limit;
	}

	public byte[] getTruncateString() {
		return truncateMessage;
	}

	public int skipObjectOrArrayMaxSizeMaxStringLength(final byte[] chars, int offset, int maxReadLimit, final ResizableByteArrayOutputStream buffer, int maxStringLength, JsonFilterMetrics metrics) {

		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;

		int maxSizeLimit = getMaxSizeLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int streamMark = getWrittenMark();

		int flushOffset = getFlushOffset();
		
		loop: while(offset < maxSizeLimit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					streamMark = buffer.size() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				buffer.write(chars, flushOffset, offset - flushOffset);
				maxSizeLimit -= offset;
				offset = skipWhitespace(chars, offset);
				maxSizeLimit += offset;


				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '{' :
			case '[' :
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}

				squareBrackets[bracketLevel] = c == '[';
				
				bracketLevel++;
				if(bracketLevel >= squareBrackets.length) {
					// grow via the field, so that callers see the grown array
					squareBrackets = grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			case '}' :
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				
				offset++;
				mark = offset;

				if(bracketLevel == levelLimit) {
					break loop;
				}

				continue;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;
				
				nextOffset++;

				if(endQuoteIndex - offset < maxStringLength + 2) {
					offset = nextOffset;

					continue;
				}

				colon:
				if(chars[nextOffset] != ':') {
					int whitespace = 0;
					if(chars[nextOffset] <= 0x20) {
						nextOffset = skipWhitespace(chars, nextOffset);

						whitespace = nextOffset - endQuoteIndex - 1;

						if(chars[nextOffset] == ':') {
							maxSizeLimit += whitespace;
							break colon;
						}
					}

					if(flushOffset <= mark) {
						streamMark = buffer.size() + mark - flushOffset; 
					}

					// was a value
					maxSizeLimit += ByteArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, digit, metrics);
					if(endQuoteIndex >= maxSizeLimit) {
						// the value does not fit: remove it, and cut the document at the mark
						buffer.setSize(streamMark);
						flushOffset = mark;
						offset = mark;
						maxSizeLimit = mark;
						break loop;
					}

					// the whitespace after the value is not written
					maxSizeLimit += whitespace;

					offset = nextOffset;
					flushOffset = nextOffset;

					continue;
				}

				// was a key
				if(flushOffset <= mark) {
					streamMark = buffer.size() + mark - flushOffset; 
				}
				buffer.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.write(':');

				nextOffset++; 
				
				offset = nextOffset;
				
				if(chars[nextOffset] <= 0x20) {
					nextOffset = skipWhitespace(chars, nextOffset);
					
					maxSizeLimit += nextOffset - offset;
				}
					
				
				flushOffset = nextOffset;
				offset = nextOffset;
				
				continue;
			}
			}
			offset++;
		}
		
		setWrittenMark(streamMark);
		setFlushOffset(flushOffset);
		setMark(mark);
		setLevel(bracketLevel);
		setMaxSizeLimit(maxSizeLimit);

		return offset;
	}
	
	public int anonymizeObjectOrArrayMaxSize(final byte[] chars, int offset, int maxReadLimit, final ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		int levelLimit = getLevel() - 1;
		int bracketLevel = getLevel();

		int maxSizeLimit = getMaxSizeLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int streamMark = getWrittenMark();

		int flushOffset = getFlushOffset();
		
		loop: while(offset < maxSizeLimit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					streamMark = buffer.size() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				buffer.write(chars, flushOffset, offset - flushOffset);
				maxSizeLimit -= offset;
				offset = skipWhitespace(chars, offset);
				maxSizeLimit += offset;


				flushOffset = offset;
				c = chars[offset];
			}
			
			switch(c) {			
			case '{' :
			case '[' :
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}

				squareBrackets[bracketLevel] = c == '[';
				
				bracketLevel++;
				if(bracketLevel >= squareBrackets.length) {
					// grow via the field, so that callers see the grown array
					squareBrackets = grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			case '}' :
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				
				offset++;
				mark = offset;

				if(bracketLevel == levelLimit) {
					break loop;
				}

				continue;
			case ',' :
				mark = offset;
				break;
			case '"': {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;
				nextOffset++;
				
				// key or value

				// skip whitespace
				// optimization: scan for highest value
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}

				if(chars[nextOffset] == ':') {
					if(nextOffset >= maxSizeLimit ) {
						offset = nextOffset;
						break loop;
					}
					
					// was a key
					offset = nextOffset + 1;

					if(nextOffset != endQuoteIndex + 1) {
						// did skip whitespace
						if(flushOffset <= mark) {
							streamMark = buffer.size() + mark - flushOffset; 
						}
						buffer.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
						buffer.write(':');
						
						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						
						flushOffset = offset;			
					}
					continue;
				}
				
				if(offset + anonymizeMessage.length > maxSizeLimit ) {
					// the anonymized value does not fit: stop at the start of the value, so that
					// neither the caller nor markToLimit(..) can include (part of) the raw value
					maxSizeLimit = offset;
					break loop;
				}
				
				// was a value
				if(flushOffset <= mark) {
					streamMark = buffer.size() + mark - flushOffset; 
				}
				buffer.write(chars, flushOffset, offset - flushOffset);
				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
				
				maxSizeLimit += nextOffset - offset - anonymizeMessage.length;
				
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
				offset = nextOffset;
				flushOffset = nextOffset;				

				continue;
			}
			default : {
				// scalar value
				if(offset + anonymizeMessage.length > maxSizeLimit) {
					// the anonymized value does not fit: stop at the start of the value
					maxSizeLimit = offset;
					break loop;
				}
				if(flushOffset <= mark) {
					streamMark = buffer.size() + mark - flushOffset; 
				}
				buffer.write(chars, flushOffset, offset - flushOffset);

				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);

				maxSizeLimit += nextOffset - offset - anonymizeMessage.length;
				
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
		
		setWrittenMark(streamMark);
		setFlushOffset(flushOffset);
		setMark(mark);
		setLevel(bracketLevel);
		setMaxSizeLimit(maxSizeLimit);

		return offset;
	}

	public int getPruneMessageLength() {
		return pruneMessage.length;
	}

	public int getAnonymizeMessageLength() {
		return anonymizeMessage.length;
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
