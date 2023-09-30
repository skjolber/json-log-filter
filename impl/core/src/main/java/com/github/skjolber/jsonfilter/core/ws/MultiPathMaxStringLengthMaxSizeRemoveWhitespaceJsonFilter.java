package com.github.skjolber.jsonfilter.core.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.FlexibleOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceBracketFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceBracketFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;

public class MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends MultiPathMaxStringLengthRemoveWhitespaceJsonFilter {

	public MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, buffer, metrics);
		}

		int bufferLength = buffer.length();

		int maxSizeLimit = offset + maxSize;
		try {
			int maxReadLimit = CharArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}
			
			CharArrayWhitespaceBracketFilter filter = new CharArrayWhitespaceBracketFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);

			filter.setLimit(maxSizeLimit);
			
			processMaxSize(chars, offset, maxReadLimit, 0, buffer, maxPathMatches, filter, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected void processMaxSize(final char[] chars, int offset, int maxReadLimit, int level, final StringBuilder buffer, int pathMatches, CharArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) {
		PathItem pathItem = this.pathItem;

		int maxSizeLimit = filter.getLimit();

		int flushOffset = filter.getStart();
		int mark = filter.getMark();
		int streamMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();

		loop:
		while(offset < maxSizeLimit) {
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
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				offset++;
				
				squareBrackets[bracketLevel] = false;
				bracketLevel++;
				
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}

				mark = offset;
				level++;

				continue;
			case '}' :
				level--;
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case '[' : {
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				squareBrackets[bracketLevel] = true;
				bracketLevel++;

				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			}
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;

				continue;
			case ',' :
				mark = offset;
				break;
			case '"' :
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;

				nextOffset++;

				if(flushOffset <= mark) {
					streamMark = buffer.length() + mark - flushOffset; 
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
					
					// was a value
					if(endQuoteIndex - offset < maxStringLength) {
						buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					} else {
						maxSizeLimit += CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, truncateStringValue, maxStringLength, metrics);
					}
					
					flushOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.append(':');

				nextOffset++;

				FilterType filterType = null;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, endQuoteIndex);
				if(pathItem.hasType()) {
					// matched
					filterType = pathItem.getType();
					
					pathItem = pathItem.constrain(level);
				}
				
				if(anyElementFilters != null && filterType == null) {
					filterType = matchAnyElements(chars, offset + 1, endQuoteIndex);
				}				

				if(chars[nextOffset] <= 0x20) {
					offset = nextOffset;
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					maxSizeLimit += nextOffset - offset;

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				}				
				
				if(filterType == null) {
					if(anyElementFilters != null) {
						flushOffset = nextOffset;
						offset = nextOffset;

						continue;
					}
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;
							flushOffset = nextOffset;

							break loop;
						}
						
						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						filter.setLevel(bracketLevel);
						filter.setMark(mark);

						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setWrittenMark(streamMark);
						
						System.out.println("Skip at " + buffer + " mark at " + mark);
						
						offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, nextOffset + 1, maxReadLimit, buffer, maxStringLength, metrics);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushOffset = filter.getStart();
						streamMark = filter.getWrittenMark();
						
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();						
					} else if(chars[nextOffset] == '"') {
						flushOffset = nextOffset;
						do {
							if(chars[nextOffset] == '\\') {
								nextOffset++;
							}
							nextOffset++;
						} while(chars[nextOffset] != '"');

						nextOffset++;
						offset = nextOffset;
					} else {
						flushOffset = nextOffset;
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					}
					continue;
				}
				
				if(filterType == FilterType.PRUNE) {
					if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
						offset = nextOffset;
						break loop;
					}
					
					if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
						offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
					} else {
						if(chars[nextOffset] == '"') {
							// quoted value
							offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
					}
					
					buffer.append(filter.getPruneMessage());
					if(metrics != null) {
						metrics.onPrune(1);
					}
					
					// adjust max size limit
					maxSizeLimit += offset - nextOffset - pruneJsonValue.length;

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
					
					mark = offset;
					flushOffset = offset;
				} else {
					if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;

							break loop;
						}

						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						
						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);
						filter.setWrittenMark(streamMark);
						
						offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, buffer, metrics);
						
						flushOffset = filter.getStart();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						streamMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();
					} else {
						if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
							offset = nextOffset;
							
							break loop;
						}
						if(chars[nextOffset] == '"') {
							// quoted value
							offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}

						buffer.append(filter.getAnonymizeMessage());

						if(metrics != null) {
							metrics.onAnonymize(1);
						}
						
						maxSizeLimit += offset - nextOffset - anonymizeJsonValue.length;

						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						mark = offset;
						flushOffset = offset;
						
					}
				}

				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						// just remove whitespace
						
						MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, buffer, level, squareBrackets, mark, streamMark, maxStringLength, truncateStringValue, metrics);

						return;
					}							
				}

				continue;
			}
			offset++;
		}
		
		if(bracketLevel > 0) {
			if(flushOffset <= mark) {
				streamMark = buffer.length() + mark - flushOffset; 
			}
			buffer.append(chars, flushOffset, offset - flushOffset);
			flushOffset = offset;
			
			
			System.out.println("Flushed at " + buffer);
			System.out.println("Mark at " + mark);
			System.out.println(new String(chars, 0, mark));
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				System.out.println("Mark lmit " + markLimit);
				System.out.println("Max size limit " + maxSizeLimit);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushOffset) {
						buffer.append(chars, flushOffset, markLimit - flushOffset);
					}
					break markLimit;
				}
				buffer.setLength(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		} else {
			buffer.append(chars, flushOffset, offset - flushOffset);
		}
	}
	
	public boolean process(final byte[] chars, int offset, int length, final ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}
		
		FlexibleOutputStream stream = new FlexibleOutputStream((length * 2) / 3, length);

		int bufferLength = output.size();

		int maxSizeLimit = offset + maxSize;
		
		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}

			ByteArrayWhitespaceBracketFilter filter = new ByteArrayWhitespaceBracketFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);

			filter.setLimit(maxSizeLimit);
			
			processMaxSize(chars, offset, maxReadLimit, 0, stream, 0, maxPathMatches, filter, metrics);

			stream.writeTo(output);

			if(metrics != null) {
				metrics.onInput(length);
				int written = output.size() - bufferLength;
				int totalSize = length;
				if(written < totalSize) {
					metrics.onMaxSize(totalSize - totalSize);
				}					
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			return false;
		}
	}

	protected void processMaxSize(final byte[] chars, int offset, int maxReadLimit, int level, final FlexibleOutputStream stream, int matches, int pathMatches, ByteArrayWhitespaceBracketFilter filter, JsonFilterMetrics metrics) throws IOException {
		PathItem pathItem = this.pathItem;

		int maxSizeLimit = filter.getLimit();

		int flushOffset = filter.getStart();
		int mark = filter.getMark();
		int streamMark = filter.getWrittenMark();
		int bracketLevel = filter.getLevel();
		
		boolean[] squareBrackets = filter.getSquareBrackets();

		loop:
		while(offset < maxSizeLimit) {
			byte c = chars[offset];
			
			if(c <= 0x20) {
				if(flushOffset <= mark) {
					streamMark = stream.size() + mark - flushOffset; 
				}
				// skip this char and any other whitespace
				stream.write(chars, flushOffset, offset - flushOffset);
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
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				offset++;
				
				squareBrackets[bracketLevel] = false;
				bracketLevel++;
				
				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}

				mark = offset;
				level++;

				continue;
			case '}' :
				level--;
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;
				
				continue;
			case '[' : {
				// check corner case
				maxSizeLimit--;
				if(offset >= maxSizeLimit) {
					break loop;
				}
				
				squareBrackets[bracketLevel] = true;
				bracketLevel++;

				if(bracketLevel >= squareBrackets.length) {
					squareBrackets = filter.grow(squareBrackets);
				}
				
				offset++;
				mark = offset;

				continue;
			}
			case ']' :
				bracketLevel--;
				maxSizeLimit++;
				if(maxSizeLimit >= maxReadLimit) {
					maxSizeLimit = maxReadLimit;
				}
				
				offset++;
				mark = offset;

				continue;
			case ',' :
				mark = offset;
				break;
			case '"' :
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);

				int endQuoteIndex = nextOffset;

				nextOffset++;

				if(flushOffset <= mark) {
					streamMark = stream.size() + mark - flushOffset; 
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
					
					// was a value
					if(endQuoteIndex - offset < maxStringLength) {
						stream.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					} else {
						maxSizeLimit += ByteArrayWhitespaceFilter.addMaxLength(chars, offset, stream, flushOffset, endQuoteIndex, truncateStringValueAsBytes, maxStringLength, filter.getDigit(), metrics);
					}
					
					flushOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				stream.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				stream.write(':');

				nextOffset++;

				FilterType filterType = null;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(chars, offset + 1, endQuoteIndex);
				if(pathItem.hasType()) {
					// matched
					filterType = pathItem.getType();
					
					pathItem = pathItem.constrain(level);
				}
				
				if(anyElementFilters != null && filterType == null) {
					filterType = matchAnyElements(chars, offset + 1, endQuoteIndex);
				}				

				if(chars[nextOffset] <= 0x20) {
					offset = nextOffset;
					do {
						nextOffset++;
					} while(chars[nextOffset] <= 0x20);

					maxSizeLimit += nextOffset - offset;

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
				}				
				
				if(filterType == null) {
					if(anyElementFilters != null) {
						flushOffset = nextOffset;
						offset = nextOffset;

						continue;
					}
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;
							flushOffset = nextOffset;

							break loop;
						}
						
						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);

						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setWrittenMark(streamMark);
						
						offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, nextOffset + 1, maxReadLimit, stream, maxStringLength, metrics);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushOffset = filter.getStart();
						streamMark = filter.getWrittenMark();
						
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();						
					} else if(chars[nextOffset] == '"') {
						flushOffset = nextOffset;
						do {
							if(chars[nextOffset] == '\\') {
								nextOffset++;
							}
							nextOffset++;
						} while(chars[nextOffset] != '"');

						nextOffset++;
						offset = nextOffset;
					} else {
						flushOffset = nextOffset;
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					}
					continue;
				}
				
				if(filterType == FilterType.PRUNE) {
					if(nextOffset + filter.getPruneMessageLength() > maxSizeLimit) {
						offset = nextOffset;
						break loop;
					}
					
					if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
						offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1);
					} else {
						if(chars[nextOffset] == '"') {
							// quoted value
							offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
					}
					
					stream.write(filter.getPruneMessage());
					if(metrics != null) {
						metrics.onPrune(1);
					}
					
					// adjust max size limit
					maxSizeLimit += offset - nextOffset - pruneJsonValue.length;

					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
					
					mark = offset;
					flushOffset = offset;
				} else {
					if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
						maxSizeLimit--;
						if(nextOffset >= maxSizeLimit) {
							offset = nextOffset;

							break loop;
						}

						squareBrackets[bracketLevel] = chars[nextOffset] == '[' ;
						bracketLevel++;

						if(bracketLevel >= squareBrackets.length) {
							squareBrackets = filter.grow(squareBrackets);
						}
						
						filter.setLimit(maxSizeLimit);
						filter.setStart(nextOffset);
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);
						filter.setWrittenMark(streamMark);
						
						offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, stream, metrics);
						
						flushOffset = filter.getStart();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						streamMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getLimit();
					} else {
						if(nextOffset + filter.getAnonymizeMessageLength() > maxSizeLimit) {
							offset = nextOffset;
							
							break loop;
						}
						if(chars[nextOffset] == '"') {
							// quoted value
							offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}

						stream.write(filter.getAnonymizeMessage());

						if(metrics != null) {
							metrics.onAnonymize(1);
						}
						
						maxSizeLimit += offset - nextOffset - anonymizeJsonValue.length;

						if(maxSizeLimit >= maxReadLimit) {
							maxSizeLimit = maxReadLimit;
						}
						
						mark = offset;
						flushOffset = offset;
						
					}
				}

				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						// just remove whitespace
						
						MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, stream, level, squareBrackets, mark, streamMark, filter.getDigit(), maxStringLength, truncateStringValueAsBytes, metrics);

						return;
					}							
				}

				continue;
			}
			offset++;
		}
		
		if(bracketLevel > 0) {
			if(flushOffset <= mark) {
				streamMark = stream.size() + mark - flushOffset; 
			}
			stream.write(chars, flushOffset, offset - flushOffset);
			flushOffset = offset;
			
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					if(markLimit >= flushOffset) {
						stream.write(chars, flushOffset, markLimit - flushOffset);
					}
					break markLimit;
				}
				stream.setCount(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, stream);
		} else {
			stream.write(chars, flushOffset, offset - flushOffset);
		}
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}