package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;

public class CharArrayWhitespaceSizeFilter extends CharArrayWhitespaceFilter {

	protected int maxSizeLimit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	
	
	protected int mark;
	protected int writtenMark;

	public CharArrayWhitespaceSizeFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
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
		this.maxSizeLimit = limit;
	}
	
	public int getMaxSizeLimit() {
		return maxSizeLimit;
	}

	public char[] getTruncateString() {
		return truncateMessage;
	}

	public int skipObjectOrArrayMaxSize(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer) {
		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;
		
		int maxSizeLimit = getMaxSizeLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int flushOffset = getFlushOffset();

		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					writtenMark = buffer.length() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

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
					boolean[] next = new boolean[squareBrackets.length + 32];
					System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
					squareBrackets = next;
				}
				
				offset++;
				mark = offset;

				continue;
			case '}' :
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;

				if(bracketLevel == levelLimit) {
					break loop;
				}

				continue;
			case ',' :
				mark = offset;
				break;
			case '"':
				offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, offset);
				continue;
			}
			offset++;
		}
		
		setWrittenMark(writtenMark);
		setFlushOffset(flushOffset);
		setMark(mark);
		setLevel(bracketLevel);
		setMaxSizeLimit(maxSizeLimit);
		
		return offset;
	}
	
	public int skipObjectOrArrayMaxSizeMaxStringLength(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer, int maxStringLength, JsonFilterMetrics metrics) {

		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;
		
		int maxSizeLimit = getMaxSizeLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int streamMark = getWrittenMark();

		int flushOffset = getFlushOffset();
		
		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					streamMark = buffer.length() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

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
					boolean[] next = new boolean[squareBrackets.length + 32];
					System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
					squareBrackets = next;
				}
				
				offset++;
				mark = offset;

				continue;
			case '}' :
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
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
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;
				
				nextOffset++;

				if(endQuoteIndex - offset < maxStringLength) {
					offset = nextOffset;

					continue;
				}

				colon:
				if(chars[nextOffset] != ':') {

					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);
						
						maxSizeLimit += nextOffset - endQuoteIndex - 1;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}

						if(chars[nextOffset] == ':') {
							break colon;
						}
					}
					
					if(flushOffset <= mark) {
						streamMark = buffer.length() + mark - flushOffset; 
					}
					
					// was a value
					maxSizeLimit += CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateMessage, maxStringLength, metrics);
					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
					
					offset = nextOffset;
					flushOffset = nextOffset;

					continue;
				}

				// was a key
				if(flushOffset <= mark) {
					streamMark = buffer.length() + mark - flushOffset; 
				}
				buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.append(':');

				nextOffset++; 
				
				offset = nextOffset;
				
				if(chars[nextOffset] <= 0x20) {
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);
					
					maxSizeLimit += nextOffset - endQuoteIndex - 1;
					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				}
					
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
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
	
	public int anonymizeObjectOrArrayMaxSize(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int bracketLevel = getLevel();
		
		int levelLimit = bracketLevel - 1;

		int maxSizeLimit = getMaxSizeLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int flushOffset = getFlushOffset();
		
		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					writtenMark = buffer.length() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, flushOffset, offset - flushOffset);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

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
					boolean[] next = new boolean[squareBrackets.length + 32];
					System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
					squareBrackets = next;
				}
				
				offset++;
				mark = offset;

				continue;
			case '}' :
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
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
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);

				if(nextOffset >= maxSizeLimit ) {
					offset = nextOffset;
					break loop;
				}
				
				nextOffset++;
				int endQuoteIndex = nextOffset;
				
				// key or value

				// skip whitespace
				// optimization: scan for highest value
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				if(chars[nextOffset] == ':') {
					// was a key
					offset = nextOffset + 1;

					if(nextOffset != endQuoteIndex) {
						// did skip whitespace
						if(flushOffset <= mark) {
							writtenMark = buffer.length() + mark - flushOffset; 
						}
						buffer.append(chars, flushOffset, endQuoteIndex - flushOffset);
						buffer.append(':');
						
						maxSizeLimit += nextOffset - endQuoteIndex;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						flushOffset = offset;
					}
					continue;
				}
				
				// was a value
				if(flushOffset <= mark) {
					writtenMark = buffer.length() + mark - flushOffset; 
				}
				buffer.append(chars, flushOffset, offset - flushOffset);
				buffer.append(anonymizeMessage);
				
				maxSizeLimit += nextOffset - offset - anonymizeMessage.length;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}					
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
				offset = nextOffset;
				flushOffset = nextOffset;				

				continue;
			}
			default : {
				// scalar value
				if(flushOffset <= mark) {
					writtenMark = buffer.length() + mark - flushOffset; 
				}
				buffer.append(chars, flushOffset, offset - flushOffset);

				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

				buffer.append(anonymizeMessage);

				maxSizeLimit += nextOffset - offset - anonymizeMessage.length;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}					
				
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
		
		setWrittenMark(writtenMark);
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
