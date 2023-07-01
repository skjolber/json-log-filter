package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class CharArrayWhitespaceBracketFilter extends CharArrayWhitespaceFilter {

	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	
	
	protected int mark;
	protected int writtenMark;

	public CharArrayWhitespaceBracketFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharArrayWhitespaceBracketFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
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

	public void closeStructure(ByteArrayOutputStream output) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				output.write(']');
			} else {
				output.write('}');
			}
		}
	}	

	public int markToLimit(byte[] chars) {
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

	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public int getLimit() {
		return limit;
	}

	public char[] getTruncateString() {
		return truncateMessage;
	}

	public int skipObjectOrArrayMaxSize(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer) {
		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;
		
		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();

		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '{' :
			case '[' :
				squareBrackets[bracketLevel] = c == '[';

				bracketLevel++;
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				bracketLevel--;

				mark = offset;

				if(bracketLevel == levelLimit) {
					offset++;
					break loop;
				}
				break;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				do {
					offset++;
				} while(chars[offset] != '"' || chars[offset - 1] == '\\');
				offset++;
				
				continue;
			}
			}
			offset++;
		}
		
		setWrittenMark(writtenMark);
		setStart(start);
		setMark(mark);
		setLevel(bracketLevel);
		setLimit(maxSizeLimit);
		
		return offset;
	}
	
	public int skipObjectOrArrayMaxSizeMaxStringLength(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer, int maxStringLength, JsonFilterMetrics metrics) {

		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;
		
		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {			
			case '{' :
			case '[' :
				squareBrackets[bracketLevel] = c == '[';

				bracketLevel++;
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				bracketLevel--;

				mark = offset;

				if(bracketLevel == levelLimit) {
					offset++;
					break loop;
				}
				break;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

				if(nextOffset - offset - 1 > maxStringLength) {
					int endQuoteIndex = nextOffset;

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] == ':') {
						
						// was a key
						if(endQuoteIndex != nextOffset) {
							// did skip whitespace

							if(start <= mark) {
								writtenMark = buffer.length() + mark - start; 
							}
							buffer.append(chars, start, endQuoteIndex - start + 1);
							
							maxSizeLimit += nextOffset - endQuoteIndex;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							start = nextOffset;
							offset = nextOffset;
							continue;
						}
					} else {
						// was a value
						if(start <= mark) {
							writtenMark = buffer.length() + mark - start; 
						}
						int aligned = CharArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);
						buffer.append(chars, start, aligned - start);
						buffer.append(getTruncateString());
						buffer.append(endQuoteIndex - aligned);
						buffer.append('"');
						
						if(metrics != null) {
							metrics.onMaxStringLength(1);
						}
						
						maxSizeLimit += nextOffset - aligned; // also accounts for skipped whitespace, if any
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						start = nextOffset;
					}
				} else {
					nextOffset++;
				}
				offset = nextOffset;

				continue;
			}
			}
			offset++;
		}
		
		setWrittenMark(writtenMark);
		setStart(start);
		setMark(mark);
		setLevel(bracketLevel);
		setLimit(maxSizeLimit);

		return offset;
	}
	
	public int anonymizeObjectOrArrayMaxSize(final char[] chars, int offset, int maxReadLimit, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int bracketLevel = getLevel();
		
		int levelLimit = bracketLevel;

		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < maxSizeLimit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					maxSizeLimit++;
				} while(chars[offset] <= 0x20);

				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {			
			case '{' :
			case '[' :
				squareBrackets[bracketLevel] = c == '[';

				bracketLevel++;
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				bracketLevel--;

				mark = offset;

				if(bracketLevel == levelLimit) {
					offset++;
					break loop;
				}
				break;
			case ',' :
				mark = offset;
				break;				
			case '"': {
				
				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');

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
						if(start <= mark) {
							writtenMark = buffer.length() + mark - start; 
						}
						buffer.append(chars, start, endQuoteIndex - start);
						buffer.append(':');
						
						maxSizeLimit += nextOffset - endQuoteIndex;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						start = offset;			
					}
					continue;
				}
				// was a value
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				buffer.append(chars, start, offset - start);
				buffer.append(anonymizeMessage);
				
				maxSizeLimit += nextOffset - offset - anonymizeMessage.length;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}					
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
				offset = nextOffset;
				start = nextOffset;				

				continue;
			}
			default : {
				// scalar value
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				buffer.append(chars, start, offset - start);

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
				start = nextOffset;
				
				continue;
			}
			
			}
			offset++;
		}
		
		setWrittenMark(writtenMark);
		setStart(start);
		setMark(mark);
		setLevel(bracketLevel);
		setLimit(maxSizeLimit);

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
