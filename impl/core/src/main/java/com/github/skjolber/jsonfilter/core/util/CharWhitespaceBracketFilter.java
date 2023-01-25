package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class CharWhitespaceBracketFilter extends CharWhitespaceFilter {

	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	
	
	protected int mark;
	protected int writtenMark;

	public CharWhitespaceBracketFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharWhitespaceBracketFilter(char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
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

	public int skipObjectOrArrayMaxSize(final char[] chars, int offset, int maxLimit, final StringBuilder buffer) {
		int levelLimit = getLevel() - 1;

		int limit = getLimit();

		int level = getLevel();
		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();

		loop: while(offset < limit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					limit++;
				} while(chars[offset] <= 0x20);

				if(limit >= maxLimit) {
					limit = maxLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {
			case '{' :
			case '[' :
				squareBrackets[level] = c == '[';

				level++;
				if(level >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				level--;

				mark = offset;

				if(level == levelLimit) {
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
		setLevel(level);
		setLimit(limit);
		
		return offset;
	}
	
	public int skipObjectMaxSizeMaxStringLength(final char[] chars, int offset, int maxLimit, final StringBuilder buffer, int maxStringLength, JsonFilterMetrics metrics) {

		int levelLimit = getLevel() - 1;

		int limit = getLimit();

		int level = getLevel();
		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < limit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					limit++;
				} while(chars[offset] <= 0x20);

				if(limit >= maxLimit) {
					limit = maxLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {			
			case '{' :
			case '[' :
				squareBrackets[level] = c == '[';

				level++;
				if(level >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				level--;

				mark = offset;

				if(level == levelLimit) {
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
							
							limit += nextOffset - endQuoteIndex;
							if(limit >= maxLimit) {
								limit = maxLimit;
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
						
						limit += nextOffset - aligned; // also accounts for skipped whitespace, if any
						if(limit >= maxLimit) {
							limit = maxLimit;
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
		setLevel(level);
		setLimit(limit);

		return offset;
	}
	
	public int anonymizeObjectOrArrayMaxSize(final char[] chars, int offset, int maxLimit, final StringBuilder buffer, JsonFilterMetrics metrics) {
		int levelLimit = getLevel() - 1;

		int limit = getLimit();

		int level = getLevel();
		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < limit) {
			char c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.length() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.append(chars, start, offset - start);
				do {
					offset++;
					limit++;
				} while(chars[offset] <= 0x20);

				if(limit >= maxLimit) {
					limit = maxLimit;
				}

				start = offset;
				c = chars[offset];
			}
			
			switch(c) {			
			case '{' :
			case '[' :
				squareBrackets[level] = c == '[';

				level++;
				if(level >= squareBrackets.length) {
					squareBrackets = grow(squareBrackets);
				}
				mark = offset;

				break;
			case '}' :
			case ']' :
				level--;

				mark = offset;

				if(level == levelLimit) {
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
						
						limit += nextOffset - endQuoteIndex;
						if(limit >= maxLimit) {
							limit = maxLimit;
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
				
				limit += nextOffset - offset - anonymizeMessage.length;
				if(limit >= maxLimit) {
					limit = maxLimit;
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

				limit += nextOffset - offset - anonymizeMessage.length;
				if(limit >= maxLimit) {
					limit = maxLimit;
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
		setLevel(level);
		setLimit(limit);

		return offset;
	}
	
}
