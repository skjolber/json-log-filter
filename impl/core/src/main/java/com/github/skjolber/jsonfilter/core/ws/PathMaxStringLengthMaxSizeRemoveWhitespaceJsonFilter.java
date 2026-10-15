package com.github.skjolber.jsonfilter.core.ws;

import java.io.IOException;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilters;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayWhitespaceSizeFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayWhitespaceSizeFilter;

public class PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter extends PathMaxStringLengthRemoveWhitespaceJsonFilter {

	public PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer, JsonFilterMetrics metrics) {
		if(isNonStructural(chars, offset, length)) {
			// rare, and cannot be handled by the regular scanners
			try {
				CharArrayRangesFilter.filterNonStructural(chars, offset, length, buffer, metrics, maxStringLength, true, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				return true;
			} catch(Exception e) {
				return false;
			}
		}

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
			
			CharArrayWhitespaceSizeFilter filter = new CharArrayWhitespaceSizeFilter(pruneJsonValue, anonymizeJsonValue, truncateStringValue);

			filter.setMaxSizeLimit(maxSizeLimit);
			filter.setFlushOffset(offset);
			filter.setMark(offset);
			filter.setWrittenMark(buffer.length());
			
			int level = processMaxSize(chars, offset, maxReadLimit, 0, buffer, maxPathMatches, filter, metrics);
			
			if(metrics != null) {
				metrics.onInput(length);
				int written = buffer.length() - bufferLength;
				int totalSize = length;
				if(level > 0) {
					// the document was cut, i.e. not only whitespace was removed
					metrics.onMaxSize(totalSize - written);
				}					
				metrics.onOutput(buffer.length() - bufferLength);
			}
			return true;
		} catch(Exception e) {
			// do not leave partial output in the buffer
			buffer.setLength(bufferLength);
			return false;
		}
	}

	protected int processMaxSize(final char[] chars, int offset, int maxReadLimit, int level, final StringBuilder buffer, int pathMatches, CharArrayWhitespaceSizeFilter filter, JsonFilterMetrics metrics) {
		PathItem pathItem = this.pathItem;

		AnyPathFilters anyPathFilters = this.anyPathFilters;

		int maxStringLength = this.maxStringLength;
		
		int maxSizeLimit = filter.getMaxSizeLimit();

		int flushOffset = filter.getFlushOffset();
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
					int whitespace = 0;
					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						whitespace = nextOffset - endQuoteIndex - 1;
						
						if(chars[nextOffset] == ':') {
							maxSizeLimit += whitespace;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							break colon;
						}
					}
					
					// was a value
					if(endQuoteIndex - offset < maxStringLength + 2) {
						buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					} else {
						maxSizeLimit += CharArrayWhitespaceFilter.addMaxLength(chars, offset, buffer, flushOffset, endQuoteIndex, filter.getTruncateMessage(), maxStringLength, metrics);
					}
					
					if(endQuoteIndex >= maxSizeLimit) {
						// the value does not fit: remove it, and cut the document at the mark
						buffer.setLength(streamMark);
						flushOffset = mark;
						offset = mark;
						
						break loop;
					}
					
					// the whitespace after the value is not written
					maxSizeLimit += whitespace;
					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
					
					flushOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				buffer.append(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				buffer.append(':');

				nextOffset++;

				FilterType filterType = null;
				
				PathItem previousPathItem = pathItem;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, endQuoteIndex);
				if(pathItem.hasType()) {
					// matched
					filterType = pathItem.getType();
					
					pathItem = pathItem.constrain(level);
				}
				
				if(anyPathFilters != null && filterType != FilterType.PRUNE) {
					// prune takes precedence over anonymize
					filterType = FilterType.strongest(filterType, anyPathFilters.matchPath(chars, offset + 1, endQuoteIndex));
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
				
				flushOffset = nextOffset;
				
				if(filterType == null) {
					if(anyPathFilters != null || previousPathItem.getLevel() < pathItem.getLevel()) {
						offset = nextOffset;

						continue;
					}
					
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
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
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);

						filter.setMaxSizeLimit(maxSizeLimit);
						filter.setFlushOffset(nextOffset);
						filter.setWrittenMark(streamMark);
						
						offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, nextOffset + 1, maxReadLimit, buffer, maxStringLength, metrics);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushOffset = filter.getFlushOffset();
						streamMark = filter.getWrittenMark();
						
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getMaxSizeLimit();
					} else if(chars[nextOffset] == '"') {
						// let the main loop handle the string value, which applies max string length
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
						offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset);
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
					maxSizeLimit += offset - nextOffset - filter.getPruneMessageLength();
					
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
						
						filter.setMaxSizeLimit(maxSizeLimit);
						filter.setFlushOffset(nextOffset);
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);
						filter.setWrittenMark(streamMark);
						
						offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, buffer, metrics);
						
						flushOffset = filter.getFlushOffset();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						streamMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getMaxSizeLimit();
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
						
						maxSizeLimit += offset - nextOffset - filter.getAnonymizeMessageLength();

						mark = offset;
						flushOffset = offset;
					}
				}
				
				if(maxSizeLimit + level > maxReadLimit) {
					maxSizeLimit = maxReadLimit - level;
				}
				
				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						// just remove whitespace
						buffer.append(chars, flushOffset, offset - flushOffset);

						return MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, buffer, bracketLevel, squareBrackets, mark, streamMark, maxStringLength, filter.getTruncateMessage(), metrics);
					}							
				}

				continue;
			}
			offset++;
		}
		
		int kept = mark;
		if(bracketLevel > 0) {
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					kept = markLimit;
					if(markLimit >= flushOffset) {
						buffer.append(chars, flushOffset, markLimit - flushOffset);
					}
					break markLimit;
				} else {
					if(mark >= flushOffset) {
						streamMark = buffer.length() + mark - flushOffset; 
						
						buffer.append(chars, flushOffset, mark - flushOffset);
					}
				}
				buffer.setLength(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, buffer);
		} else {
			buffer.append(chars, flushOffset, offset - flushOffset);
		}
		
		// report a cut only if content was dropped, not just whitespace
		return bracketLevel > 0 && MaxSizeJsonFilter.hasContent(chars, kept, maxReadLimit) ? bracketLevel : 0;
	}
	
	public boolean process(final byte[] chars, int offset, int length, final ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(isNonStructural(chars, offset, length)) {
			// rare, and cannot be handled by the regular scanners
			try {
				ByteArrayRangesFilter.filterNonStructural(chars, offset, length, output, metrics, maxStringLength, true, pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);
				return true;
			} catch(Exception e) {
				return false;
			}
		}

		if(!mustConstrainMaxSize(length)) {
			return super.process(chars, offset, length, output, metrics);
		}
		
		int bufferLength = output.size();

		int maxSizeLimit = offset + maxSize;
		
		try {
			int maxReadLimit = ByteArrayWhitespaceFilter.skipWhitespaceFromEnd(chars, length + offset);
			if(maxSizeLimit >= maxReadLimit) {
				maxSizeLimit = maxReadLimit;
			}

			ByteArrayWhitespaceSizeFilter filter = new ByteArrayWhitespaceSizeFilter(pruneJsonValueAsBytes, anonymizeJsonValueAsBytes, truncateStringValueAsBytes);

			filter.setMaxSizeLimit(maxSizeLimit);
			filter.setFlushOffset(offset);
			filter.setMark(offset);
			filter.setWrittenMark(output.size());
			
			int level = processMaxSize(chars, offset, maxReadLimit, 0, output, maxPathMatches, filter, metrics);

			if(metrics != null) {
				metrics.onInput(length);
				int written = output.size() - bufferLength;
				int totalSize = length;
				if(level > 0) {
					// the document was cut, i.e. not only whitespace was removed
					metrics.onMaxSize(totalSize - written);
				}					
				metrics.onOutput(output.size() - bufferLength);
			}

			return true;
		} catch(Exception e) {
			// do not leave partial output in the buffer
			output.setSize(bufferLength);
			return false;
		}
	}

	protected int processMaxSize(final byte[] chars, int offset, int maxReadLimit, int level, final ResizableByteArrayOutputStream stream, int pathMatches, ByteArrayWhitespaceSizeFilter filter, JsonFilterMetrics metrics) throws IOException {
		PathItem pathItem = this.pathItem;

		AnyPathFilters anyPathFilters = this.anyPathFilters;
		
		int maxStringLength = this.maxStringLength;

		int maxSizeLimit = filter.getMaxSizeLimit();

		int flushOffset = filter.getFlushOffset();
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
					int whitespace = 0;
					if(chars[nextOffset] <= 0x20) {
						do {
							nextOffset++;
						} while(chars[nextOffset] <= 0x20);

						whitespace = nextOffset - endQuoteIndex - 1;
						
						if(chars[nextOffset] == ':') {
							maxSizeLimit += whitespace;
							if(maxSizeLimit >= maxReadLimit) {
								maxSizeLimit = maxReadLimit;
							}
							break colon;
						}
					}
					
					// was a value
					if(endQuoteIndex - offset < maxStringLength + 2) {
						stream.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
					} else {
						maxSizeLimit += ByteArrayWhitespaceFilter.addMaxLength(chars, offset, stream, flushOffset, endQuoteIndex, filter.getTruncateMessage(), maxStringLength, filter.getDigit(), metrics);
					}
					
					if(endQuoteIndex >= maxSizeLimit) {
						// the value does not fit: remove it, and cut the document at the mark
						stream.setSize(streamMark);
						flushOffset = mark;
						offset = mark;
						
						break loop;
					}
					
					// the whitespace after the value is not written
					maxSizeLimit += whitespace;
					if(maxSizeLimit >= maxReadLimit) {
						maxSizeLimit = maxReadLimit;
					}
					
					flushOffset = nextOffset;
					offset = nextOffset;
					
					continue;
				}
				
				stream.write(chars, flushOffset, endQuoteIndex - flushOffset + 1);
				stream.write(':');

				nextOffset++;

				FilterType filterType = null;
				
				PathItem previousPathItem = pathItem;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(level, chars, offset + 1, endQuoteIndex);
				if(pathItem.hasType()) {
					// matched
					filterType = pathItem.getType();
					
					pathItem = pathItem.constrain(level);
				}
				
				if(anyPathFilters != null && filterType != FilterType.PRUNE) {
					// prune takes precedence over anonymize
					filterType = FilterType.strongest(filterType, anyPathFilters.matchPath(chars, offset + 1, endQuoteIndex));
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
				
				flushOffset = nextOffset;

				if(filterType == null) {
					if(anyPathFilters != null || previousPathItem.getLevel() < pathItem.getLevel()) {
						offset = nextOffset;

						continue;
					}
					// skip here
					if(chars[nextOffset] == '{' || chars[nextOffset] == '[') {
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
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);

						filter.setMaxSizeLimit(maxSizeLimit);
						filter.setFlushOffset(flushOffset);
						filter.setWrittenMark(streamMark);
						
						offset = filter.skipObjectOrArrayMaxSizeMaxStringLength(chars, nextOffset + 1, maxReadLimit, stream, maxStringLength, metrics);

						bracketLevel = filter.getLevel();
						mark = filter.getMark();

						flushOffset = filter.getFlushOffset();
						streamMark = filter.getWrittenMark();
						
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getMaxSizeLimit();						
					} else if(chars[nextOffset] == '"') {
						// let the main loop handle the string value, which applies max string length
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
						offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset);
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
					maxSizeLimit += offset - nextOffset - filter.getPruneMessageLength();

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
						
						filter.setMaxSizeLimit(maxSizeLimit);
						filter.setFlushOffset(flushOffset);
						filter.setLevel(bracketLevel);
						filter.setMark(nextOffset + 1);
						filter.setWrittenMark(streamMark);
						
						offset = filter.anonymizeObjectOrArrayMaxSize(chars, nextOffset + 1, maxReadLimit, stream, metrics);
						
						flushOffset = filter.getFlushOffset();
						bracketLevel = filter.getLevel();
						mark = filter.getMark();
						streamMark = filter.getWrittenMark();
						squareBrackets = filter.getSquareBrackets();
						maxSizeLimit = filter.getMaxSizeLimit();
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
						
						maxSizeLimit += offset - nextOffset - filter.getAnonymizeMessageLength();
						
						mark = offset;
						flushOffset = offset;
					}
				}
				
				if(maxSizeLimit + level > maxReadLimit) {
					maxSizeLimit = maxReadLimit - level;
				}							

				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						// just remove whitespace
						stream.write(chars, flushOffset, offset - flushOffset);

						return MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize(chars, offset, maxSizeLimit, maxReadLimit, stream, bracketLevel, squareBrackets, mark, streamMark, filter.getDigit(), maxStringLength, truncateStringValueAsBytes, metrics);
					}							
				}

				continue;
			}
			offset++;
		}
		
		int kept = mark;
		if(bracketLevel > 0) {
			markLimit:
			if(mark <= maxSizeLimit) {
				int markLimit = MaxSizeJsonFilter.markToLimit(chars, offset, maxReadLimit, maxSizeLimit, mark);
				if(markLimit != -1 && markLimit <= maxSizeLimit) {
					kept = markLimit;
					if(markLimit >= flushOffset) {
						stream.write(chars, flushOffset, markLimit - flushOffset);
					}
					break markLimit;
				} else {
					if(mark >= flushOffset) {
						streamMark = stream.size() + mark - flushOffset; 
						
						stream.write(chars, flushOffset, mark - flushOffset);
					}
				}
				stream.setSize(streamMark);
			}
			MaxSizeJsonFilter.closeStructure(bracketLevel, squareBrackets, stream);
		} else {
			stream.write(chars, flushOffset, offset - flushOffset);
		}
		
		// report a cut only if content was dropped, not just whitespace
		return bracketLevel > 0 && MaxSizeJsonFilter.hasContent(chars, kept, maxReadLimit) ? bracketLevel : 0;
	}

	@Override
	public boolean isRemovingWhitespace() {
		return true;
	}

}
