package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

public class CharArrayRangesFilter extends AbstractRangesFilter {
	
	protected static final char[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS = FILTER_PRUNE_MESSAGE_JSON.toCharArray();
	protected static final char[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS = FILTER_ANONYMIZE_MESSAGE.toCharArray();
	protected static final char[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS = FILTER_TRUNCATE_MESSAGE.toCharArray();

	protected final char[] pruneMessage;
	protected final char[] anonymizeMessage;
	protected final char[] truncateMessage;
	
	public CharArrayRangesFilter(int initialCapacity, int length) {
		this(initialCapacity, length, DEFAULT_FILTER_PRUNE_MESSAGE_CHARS, DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS, DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
	}

	public CharArrayRangesFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage, char[] truncateMessage) {
		super(initialCapacity, length);
		this.pruneMessage = pruneMessage;
		this.anonymizeMessage = anonymizeMessage;
		this.truncateMessage = truncateMessage;
	}

	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		
		if(metrics != null) {
			metrics.onInput(length);
		}
		
		length += offset;
		
		int bufferSize = buffer.length();
		
		for(int i = 0; i < filterIndex; i += 3) {
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(anonymizeMessage);
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(pruneMessage);
				if(metrics != null) {
					metrics.onPrune(1);
				}
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.append(chars, offset, filter[i] - offset);
				
				if(metrics != null) {
					metrics.onMaxSize(length - filter[i]);
				}
			} else {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(truncateMessage);
				buffer.append(-filter[i+2]);
				
				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.append(chars, offset, length - offset);
		}
		
		if(metrics != null) {
			metrics.onOutput(buffer.length() - bufferSize);
		}
	}

	public void filter(final char[] chars, int offset, int length, final StringBuilder buffer) {
		length += offset;
		
		for(int i = 0; i < filterIndex; i += 3) {
			if(filter[i+2] == FILTER_ANON) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(anonymizeMessage);
			} else if(filter[i+2] == FILTER_PRUNE) {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(pruneMessage);
			} else if(filter[i+2] == FILTER_DELETE) {
				buffer.append(chars, offset, filter[i] - offset);
			} else {
				buffer.append(chars, offset, filter[i] - offset);
				buffer.append(truncateMessage);
				buffer.append(-filter[i+2]);
			}
			offset = filter[i + 1];
		}
		
		if(offset < length) {
			buffer.append(chars, offset, length - offset);
		}
	}

	public static int skipObject(char[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
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
					offset = scanQuotedValue(chars, offset);
					continue;
				}
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
	
	public static int skipArray(char[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
				case '[' : {
					level++;
					break;
				}
				case ']' : {
					level--;
					
					if(level == 0) {
						return offset + 1;
					}
					break;
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
	
	public static int skipObjectMaxStringLength(char[] chars, int offset, int maxStringLength, CharArrayRangesFilter filter) {
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
				default :
			}
			offset++;
		}
	}
	

	public static int skipArrayMaxStringLength(char[] chars, int offset, int maxStringLength, CharArrayRangesFilter filter) {
		int level = 1;

		while(true) {
			switch(chars[offset]) {
				case '[' : {
					level++;
					break;
				}
				case ']' : {
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

	public static int skipObjectOrArray(char[] chars, int offset) {
		int level = 1;

		while(true) {
			switch(chars[++offset]) {
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
	
	public static final int scanBeyondQuotedValue(final char[] chars, int offset) {
		return scanQuotedValue(chars, offset) + 1;
	}

	public static final int scanQuotedValue(final char[] chars, int offset) {
		while(chars[++offset] != '"');
		if(chars[offset - 1] != '\\') {
			return offset;
		}
		
		return scanEscapedValue(chars, offset);	
	}

	public static int scanEscapedValue(final char[] chars, int offset) {
		while(true) {
			// is there an even number of quotes behind?
			int slashOffset = offset - 2;
			while(chars[slashOffset] == '\\') {
				slashOffset--;
			}
			if((offset - slashOffset) % 2 == 1) {
				return offset;
			}
			
			while(chars[++offset] != '"');
			
			if(chars[offset - 1] != '\\') {
				return offset;
			}			
		}
	}	
	
	public static final int scanBeyondUnquotedValue(final char[] chars, int offset) {
		while(true) {
			switch(chars[++offset]) {
			case ',':
			case '}':
			case ']': 
			case ' ': 
			case '\t': 
			case '\r': 
			case '\n': 
				return offset;
				default:
			}
		}
	}

	public static int anonymizeObjectOrArray(char[] chars, int offset, CharArrayRangesFilter filter) {
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
	
	
	public boolean addMaxLength(char[] chars, int start, int end, int length) {
		// account for code points and escaping
		
		int alignedStart = getStringAlignment(chars, start);
		
		length += start - alignedStart;
		
		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = end - alignedStart - truncateMessage.length - lengthToDigits(length);
		if(remove > 0) {
			super.addMaxLength(alignedStart, end, length);
			
			this.removedLength += remove;
			
			return true;
		}
		return false;
	}
	
	public void addAnon(int start, int end) {
		super.addAnon(start, end);
		
		this.removedLength += end - start - anonymizeMessage.length;
	}
	
	public void add(FilterType filterType, int start, int end) {
		if(filterType == FilterType.ANON) {
			addAnon(start, end);
		} else if(filterType == FilterType.PRUNE) {
			addPrune(start, end);
		}
	}
	
	public void addPrune(int start, int end) {
		super.addPrune(start, end);
		
		this.removedLength += end - start - pruneMessage.length;
	}

	public void addDelete(int start, int end) {
		super.addDelete(start, end);
		
		this.removedLength += end - start;
	}
	
	public int getPruneMessageLength() {
		return pruneMessage.length;
	}

	public int getAnonymizeMessageLength() {
		return anonymizeMessage.length;
	}
	
	
	public static int getStringAlignment(char[] chars, int start) {
		// account for code points and escaping
		
		// A high surrogate precedes a low surrogate. Together they make up a codepoint.
		if(Character.isLowSurrogate(chars[start])) {
			start--;
		} else {
			// \ u
			// \ uX
			// \ uXX
			// \ uXXX
			//
			// where X is hex

			char peek = chars[start];
			// check for unicode encoding. That means the peek char must be a hex
			if( (peek >= '0' && peek <= '9') || (peek >= 'A' && peek <= 'F')) {
				int index = start - 1; // index of last character which is included
				
				// absolute minimum is length 8:
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
		}
		return start;
	}


}
