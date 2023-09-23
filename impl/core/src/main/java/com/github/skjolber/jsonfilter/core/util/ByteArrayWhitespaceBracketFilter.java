package com.github.skjolber.jsonfilter.core.util;

import java.io.ByteArrayOutputStream;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class ByteArrayWhitespaceBracketFilter extends ByteArrayWhitespaceFilter {

	protected int limit;

	protected boolean[] squareBrackets = new boolean[32];
	protected int level;	
	
	protected int mark;
	protected int writtenMark;

	public ByteArrayWhitespaceBracketFilter() {
		this(DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public ByteArrayWhitespaceBracketFilter(byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
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

	public byte[] getTruncateString() {
		return truncateMessage;
	}

	public int skipObjectOrArrayMaxSize(final byte[] chars, int offset, int maxReadLimit, final ByteArrayOutputStream buffer) {
		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;
		
		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();

		loop: while(offset < maxSizeLimit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.size() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.write(chars, start, offset - start);
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
				do {
					if(chars[offset] == '\\') {
						offset++;
					}
					offset++;
				} while(chars[offset] != '"');
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
	
	public int skipObjectOrArrayMaxSizeMaxStringLength(final byte[] chars, int offset, int maxReadLimit, final ByteArrayOutputStream buffer, int maxStringLength, JsonFilterMetrics metrics) {

		int bracketLevel = getLevel();
		int levelLimit = bracketLevel - 1;

		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < maxSizeLimit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.size() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.write(chars, start, offset - start);
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
					if(chars[nextOffset] == '\\') {
						nextOffset++;
					}
					nextOffset++;
				} while(chars[nextOffset] != '"');

				if(nextOffset - offset - 1 > maxStringLength) {
					int endQuoteIndex = nextOffset;

					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					if(chars[nextOffset] != ':') {
						// was a value
						int aligned = ByteArrayRangesFilter.getStringAlignment(chars, offset + maxStringLength + 1);

						int skipped = endQuoteIndex - aligned;
						
						int remove = skipped - truncateMessage.length - AbstractRangesFilter.lengthToDigits(skipped);

						if(remove > 0) {
							if(start <= mark) {
								writtenMark = buffer.size() + mark - start; 
							}
							
							buffer.write(chars, start, aligned - start);
							byte[] truncateString = getTruncateString();
							buffer.write(truncateString, 0, truncateString.length);
							buffer.write(skipped);
							buffer.write('"');
							
							if(metrics != null) {
								metrics.onMaxStringLength(1);
							}
							
							maxSizeLimit += nextOffset - aligned; // also accounts for skipped whitespace, if any
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							start = nextOffset;
							offset = nextOffset;

							continue;
						}
					}
					// key or not long enough value
					if(endQuoteIndex + 1 != nextOffset) {
						// did skip whitespace

						if(start <= mark) {
							writtenMark = buffer.size() + mark - start; 
						}
						buffer.write(chars, start, endQuoteIndex - start + 1);
						
						maxSizeLimit += nextOffset - endQuoteIndex;
						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						start = nextOffset;
						offset = nextOffset;
						continue;
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
	
	public int anonymizeObjectOrArrayMaxSize(final byte[] chars, int offset, int maxReadLimit, final ByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
		int levelLimit = getLevel();
		int bracketLevel = getLevel();

		int maxSizeLimit = getLimit();

		boolean[] squareBrackets = getSquareBrackets();

		int mark = getMark();
		int writtenMark = getWrittenMark();

		int start = getStart();
		
		loop: while(offset < maxSizeLimit) {
			byte c = chars[offset];
			if(c <= 0x20) {
				if(start <= mark) {
					writtenMark = buffer.size() + mark - start; 
				}
				// skip this char and any other whitespace
				buffer.write(chars, start, offset - start);
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
				int nextOffset = offset;
				do {
					if(chars[nextOffset] == '\\') {
						nextOffset++;
					}
					nextOffset++;
				} while(chars[nextOffset] != '"');

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
							writtenMark = buffer.size() + mark - start; 
						}
						buffer.write(chars, start, endQuoteIndex - start);
						buffer.write(':');
						
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
					writtenMark = buffer.size() + mark - start; 
				}
				buffer.write(chars, start, offset - start);
				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);
				
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
					writtenMark = buffer.size() + mark - start; 
				}
				buffer.write(chars, start, offset - start);

				int nextOffset = offset;
				do {
					nextOffset++;
				} while(chars[nextOffset] != ',' && chars[nextOffset] != '}' && chars[nextOffset] != ']');

				buffer.write(anonymizeMessage, 0, anonymizeMessage.length);

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
