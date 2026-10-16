package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class CharArrayRangesSizeFilter extends CharArrayRangesFilter {

	protected boolean[] squareBrackets = new boolean[32];
	protected int mark;
	protected int level;
	protected int maxSizeLimit = -1;

	public CharArrayRangesSizeFilter(int initialCapacity, int length, char[] pruneMessage, char[] anonymizeMessage,
			char[] truncateMessage) {
		super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
	}

	/** true if prune and anonymize messages make the output larger than the input, so that the rest of the document cannot be kept without max size checks */
	protected boolean replacementGrowth;

	public void setReplacementGrowth(boolean replacementGrowth) {
		this.replacementGrowth = replacementGrowth;
	}

	public boolean isReplacementGrowth() {
		return replacementGrowth;
	}

	/**
	 * A copy of {@link CharArrayRangesFilter#addMaxLength(char[], int, int, int)} for the max size scanning loop of
	 * MaxStringLengthMaxSizeJsonFilter. HotSpot profiles branches per method, so if both that loop and the
	 * plain max string length loop, which takes over when the rest of the document fits, inline the same method,
	 * they share the profile of the rarely taken alignment branches, and that profile decides how each loop is
	 * compiled. With its own copy, each loop gets its own profile and code.
	 *
	 * @param chars input
	 * @param start index of the first char to remove
	 * @param end index of the closing quote
	 * @param length number of chars removed, excluding the alignment
	 * @return true if the value was truncated
	 */

	public boolean addMaxLengthMaxSize(char[] chars, int start, int end, int length) {
		int alignedStart = getStringAlignmentMaxSize(chars, start);

		length += start - alignedStart;

		// if truncate message + digits is smaller than the actual payload, trim it.
		int remove = end - alignedStart - truncateMessage.length - lengthToDigits(length);
		if(remove > 0) {
			addMaxLength(alignedStart, end, length);

			this.removedLength += remove;

			return true;
		}
		return false;
	}

	/**
	 * The alignment for {@link #addMaxLengthMaxSize(char[], int, int, int)}.
	 * The common case comes first: the cut can only split a surrogate pair or an escape if the char at
	 * the cut is a low surrogate, or a backslash is among the six chars before it.
	 * The rest is left to {@link #getStringAlignment(char[], int)}, which rarely runs.
	 * <br>
	 * This is a separate method so that where the call is cold, C2 keeps it out of line, like the call to
	 * getStringAlignment(..) it replaces. Inlining it there changes how the max size loop is compiled.
	 *
	 * @param chars input
	 * @param start index of the first char to remove
	 * @return start, moved backwards if needed
	 */

	private static int getStringAlignmentMaxSize(char[] chars, int start) {
		if(start > 6
				&& chars[start - 1] != '\\' && chars[start - 2] != '\\' && chars[start - 3] != '\\'
				&& chars[start - 4] != '\\' && chars[start - 5] != '\\' && chars[start - 6] != '\\'
				&& !Character.isLowSurrogate(chars[start])) {
			return start;
		}
		return getStringAlignment(chars, start);
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

	@Override
	public void closeStructure(final StringBuilder buffer) {
		for(int i = level - 1; i >= 0; i--) {
			if(squareBrackets[i]) {
				buffer.append(']');
			} else {
				buffer.append('}');
			}
		}
	}

	public int skipObjectOrArrayMaxSizeMaxStringLength(char[] chars, int offset, int maxSizeLimit, int maxReadLimit, int maxStringLength) {
		int level = getLevel();
		int levelLimit = level - 1;
		
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '{' :
				case '[' :
				{
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = grow(squareBrackets);
					}

					offset++;
					mark = offset;

					continue;
				}
				case ']' :
				case '}' : {
					level--;
					maxSizeLimit++;

					offset++;
					mark = offset;

					if(level == levelLimit) {
						break loop;
					}
					continue;
				}
				case ',' :
					mark = offset;
					break;
				case '"' : {
					int nextOffset = scanBeyondQuotedValue(chars, offset);

					if(nextOffset - offset <= maxStringLength) {
						offset = nextOffset;
						
						continue;
					}
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
						
						int end = nextOffset - 1;
						if(chars[nextOffset] <= 0x20) {
							// fast-forward over whitespace
							// optimization: scan for highest value
							do {
								nextOffset++;
							} while(chars[nextOffset] <= 0x20);

							if(chars[nextOffset] == ':') {
								// was a key
								offset = nextOffset + 1;
								continue;
							}
						}
						
						if(offset + maxStringLength > maxSizeLimit) {
							offset = nextOffset;
							
							break loop;
						}

						int removedLength = getRemovedLength();
						if(addMaxLength(chars, offset + maxStringLength - 1, end, -(offset + maxStringLength - end - 1))) {
							// increment limit since we removed something
							maxSizeLimit += getRemovedLength() - removedLength;
	
							if(maxSizeLimit > maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							
							if(nextOffset > maxSizeLimit) {
								maxSizeLimit -= getRemovedLength() - removedLength;
										
								removeLastFilter();
								
								offset = nextOffset;
								
								break loop;
							}
						}
						
						offset = nextOffset;
						
						if(nextOffset <= maxSizeLimit) {
							mark = offset;
						}
					}
					
					continue;
				}
				default : // do nothing
			}
			offset++;
		}
		
		setLevel(level);
		setMark(mark);
		setMaxSizeLimit(maxSizeLimit);

		return offset;
	}

	public int anonymizeSubtree(char[] chars, int offset, int maxSizeLimit) {
		int levelLimit = getLevel();
		
		int level = getLevel();
		
		boolean[] squareBrackets = getSquareBrackets();
		int mark = getMark();

		loop:
		while(offset < maxSizeLimit) {
			switch(chars[offset]) {
				case '[' : 
				case '{' : {
					maxSizeLimit--;
					if(offset >= maxSizeLimit) {
						break loop;
					}
					
					squareBrackets[level] = chars[offset] == '[';
					
					level++;
					if(level >= squareBrackets.length) {
						squareBrackets = grow(squareBrackets);
					}

					offset++;
					mark = offset;

					continue;
				}
				case ']' : 
				case '}' : {
					level--;
					maxSizeLimit++;
					
					offset++;
					mark = offset;
					
					if(level == levelLimit) {
						// level same as before
						setMark(mark);
						setMaxSizeLimit(maxSizeLimit);

						return offset;
					}
					continue;
				}
				case ',' : {
					mark = offset;
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
							if(offset + getAnonymizeMessageLength() <= maxSizeLimit) {
								
								int removedLength = getRemovedLength();
								addAnon(offset, nextOffset);
								maxSizeLimit += getRemovedLength() - removedLength;
								
								if(nextOffset <= maxSizeLimit) {
									mark = nextOffset;
								} else {
									maxSizeLimit -= getRemovedLength() - removedLength;
									
									removeLastFilter();
									break loop;
								}
							} else {
								// the anonymized value does not fit: stop at the start of the value, so that
								// neither the caller nor markToLimit(..) can include the raw value
								maxSizeLimit = offset;
								break loop;
							}

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
								if(offset + getAnonymizeMessageLength() <= maxSizeLimit) {
									
									int removedLength = getRemovedLength();
									addAnon(offset, end);
									maxSizeLimit += getRemovedLength() - removedLength;
									
									if(end <= maxSizeLimit) {
										mark = end;
									} else {
										maxSizeLimit -= getRemovedLength() - removedLength;

										removeLastFilter();
										break loop;
									}
								} else {
									// the anonymized value does not fit: stop at the start of the value, so that
									// neither the caller nor markToLimit(..) can include the raw value
									maxSizeLimit = offset;

									break loop;
								}
								offset = nextOffset;
							}
						}
					}
					
					continue;
				}
				default : {
					// scalar value
					int nextOffset = scanBeyondUnquotedValue(chars, offset);
					
					if(offset + getAnonymizeMessageLength() <= maxSizeLimit) {
						
						int removedLength = getRemovedLength();
						addAnon(offset, nextOffset);
						maxSizeLimit += getRemovedLength() - removedLength;
						
						if(nextOffset <= maxSizeLimit) {
							mark = nextOffset;
						} else {
							maxSizeLimit -= getRemovedLength() - removedLength;

							removeLastFilter();
							break loop;
						}
					} else {
						// the anonymized value does not fit: stop at the start of the value, so that
						// neither the caller nor markToLimit(..) can include the raw value
						maxSizeLimit = offset;

						break loop;
					}

					offset = nextOffset;
					
					continue;
				}
			}
			offset++;
		}

		setLevel(level);
		setMark(mark);
		setMaxSizeLimit(maxSizeLimit);

		return offset;
	}

	public void setMaxSizeLimit(int maxSizeLimit) {
		this.maxSizeLimit = maxSizeLimit;
	}
	
	public int getMaxSizeLimit() {
		return maxSizeLimit;
	}
}
