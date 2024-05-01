package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class SingleAnyPathJsonFilter extends AbstractRangesSingleCharArrayAnyPathJsonFilter {

	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}
	
	public SingleAnyPathJsonFilter(int maxPathMatches, String expression, FilterType type) {
		this(maxPathMatches, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	protected SingleAnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression,
			FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);
		try {
			
			if(maxPathMatches != -1) {
				if(pathBytes == STAR_BYTES) {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, maxPathMatches, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, maxPathMatches, filter);
					} else {
						throw new IllegalStateException();
					}
				} else {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, maxPathMatches, pathChars, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, maxPathMatches, pathChars, filter);
					} else {
						throw new IllegalStateException();
					}
				}
			} else {
				if(pathBytes == STAR_BYTES) {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, filter);
					} else {
						throw new IllegalStateException();
					}
				} else {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, pathChars, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, pathChars, filter);
					} else {
						throw new IllegalStateException();
					}
				}
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}


	@Override
	public ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(maxPathMatches, length);
		try {
			if(maxPathMatches != -1) {
				if(pathBytes == STAR_BYTES) {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, maxPathMatches, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, maxPathMatches, filter);
					} else {
						throw new IllegalStateException();
					}
				} else {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, maxPathMatches, pathBytes, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, maxPathMatches, pathBytes, filter);
					} else {
						throw new IllegalStateException();
					}
				}
			} else {
				if(pathBytes == STAR_BYTES) {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, filter);
					} else {
						throw new IllegalStateException();
					}
				} else {
					if(filterType == FilterType.ANON) {
						rangesAnonymizeAnyPath(chars, offset, offset + length, pathBytes, filter);
					} else if(filterType == FilterType.PRUNE) {
						rangesPruneAnyPath(chars, offset, offset + length, pathBytes, filter);
					} else {
						throw new IllegalStateException();
					}
				}
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static <T extends CharArrayRangesFilter> T rangesAnyPath(final char[] chars, int offset, int limit, int pathMatches, char[] path, FilterType filterType, T filter) {
		while(offset < limit) {
			if(chars[offset] == '"') {
				int nextOffset = CharArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int quoteIndex = nextOffset;
				
				nextOffset++;
				
				// is this a field name or a value? A field name must be followed by a colon
				if(chars[nextOffset] != ':') {
					// skip over whitespace
					
					// optimization: scan for highest value
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
						nextOffset++;
					}
					
					if(chars[nextOffset] != ':') {
						// was a text value
						offset = nextOffset;
						
						continue;
					}
				}

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				if(path == STAR_CHARS || matchPath(chars, offset + 1, quoteIndex, path)) {
					switch(chars[nextOffset]) {
						case '[':
						case '{':
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
							} else {
								offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
							}
							break;
						case '"': {
							offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							filter.add(filterType, nextOffset, offset);
							break;
						}
						case 't': 
						case 'n': {
							offset = nextOffset + 4;
							filter.add(filterType, nextOffset, offset);
							break;
						}
						case 'f': {
							offset = nextOffset + 5;
							filter.add(filterType, nextOffset, offset);
							break;
						}
						default: {
							offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
							filter.add(filterType, nextOffset, offset);
						}
					}
					
					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							return filter; // done filtering
						}
					}					
				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}

		return filter;
	}

	public static <T extends ByteArrayRangesFilter> T rangesAnyPath(final byte[] chars, int offset, int limit, int pathMatches, byte[] path, FilterType filterType, T filter) {
		while(offset < limit) {
			if(chars[offset] == '"') {
				int nextOffset = ByteArrayRangesFilter.scanQuotedValue(chars, offset);
				
				int quoteIndex = nextOffset;
				
				nextOffset++;
				
				// is this a field name or a value? A field name must be followed by a colon
				if(chars[nextOffset] != ':') {
					// skip over whitespace
					
					// optimization: scan for highest value
					// space: 0x20
					// tab: 0x09
					// carriage return: 0x0D
					// newline: 0x0A

					while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
						nextOffset++;
					}
					
					if(chars[nextOffset] != ':') {
						// was a text value
						offset = nextOffset;
						
						continue;
					}
				}

				nextOffset++;
				
				// skip whitespace
				while(chars[nextOffset] <= 0x20) {
					nextOffset++;
				}
				
				if(path == STAR_BYTES || matchPath(chars, offset + 1, quoteIndex, path)) {
					if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
						} else {
							offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
						}
					} else {
						if(chars[nextOffset] == '"') {
							// quoted value
							offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
						} else {
							offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						}
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset);
						} else {
							filter.addAnon(nextOffset, offset);
						}
					}	
					
					if(pathMatches != -1) {
						pathMatches--;
						if(pathMatches == 0) {
							return filter; // done filtering
						}
					}					
				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}

		return filter;
	}
	
	public static void rangesAnonymizeAnyPath(final char[] chars, int offset, int limit, char[] path, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addAnon(nextOffset, offset);
						
						continue;
					}
					default: {
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addAnon(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;
					}
				}
			} else {
				offset = nextOffset + 1;
			}
		}
	}
	

	public static void rangesPruneAnyPath(final char[] chars, int offset, int limit, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addPrune(nextOffset, offset);
					
					continue;
				}
				default: {
					offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
				}
			}
		}
	}
	
	public static void rangesAnonymizeAnyPath(final char[] chars, int offset, int limit, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addAnon(nextOffset, offset);
					
					continue;
				}
				case 't': 
				case 'n': {
					offset = nextOffset + 4;
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
					break;
				}
				case 'f': {
					offset = nextOffset + 5;
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
					break;
				}
				default: {
					offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
				}
			}
		}
	}
	
	public static void rangesPruneAnyPath(final char[] chars, int offset, int limit, char[] path, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addPrune(nextOffset, offset);
						
						continue;
					}
					default: {
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addPrune(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}
				}
			} else {
				offset = nextOffset + 1;
			}
		}
	}	
	
	public static void rangesPruneAnyPath(final byte[] chars, int offset, int limit, byte[] path, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addPrune(nextOffset, offset);
						
						continue;
					}
					default: {
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addPrune(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
			} else {
				offset = nextOffset + 1;
			}
		}
	}

	public static void rangesAnonymizeAnyPath(final byte[] chars, int offset, int limit, byte[] path, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addAnon(nextOffset, offset);
						
						continue;
					}
					default: {
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addAnon(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
			} else {
				offset = nextOffset + 1;
			}
		}
	}
	

	public static void rangesPruneAnyPath(final byte[] chars, int offset, int limit, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addPrune(nextOffset, offset);
					
					continue;
				}
				default: {
					offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addPrune(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}
			}
		}
	}
	
	public static void rangesAnonymizeAnyPath(final byte[] chars, int offset, int limit, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addAnon(nextOffset, offset);
					
					continue;
				}
				default: {
					offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}
			}
		}
	}
	
	public static void rangesAnonymizeAnyPath(final char[] chars, int offset, int limit, int maxPathMatches, char[] path, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addAnon(nextOffset, offset);
						
						break;
					}
					default: {
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addAnon(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
				
				maxPathMatches--;
				if(maxPathMatches == 0) {
					break; // done filtering
				}

			} else {
				offset = nextOffset + 1;
			}
		}
	}
	

	public static void rangesPruneAnyPath(final char[] chars, int offset, int limit, int maxPathMatches, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addPrune(nextOffset, offset);
					
					break;
				}
				default: {
					offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addPrune(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}

			}
			
			maxPathMatches--;
			if(maxPathMatches == 0) {
				break; // done filtering
			}

		}
	}
	
	public static void rangesAnonymizeAnyPath(final char[] chars, int offset, int limit, int maxPathMatches, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addAnon(nextOffset, offset);
					
					break;
				}
				default: {
					offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}

			}
			maxPathMatches--;
			if(maxPathMatches == 0) {
				break; // done filtering
			}
		}
	}
	
	public static void rangesPruneAnyPath(final char[] chars, int offset, int limit, int maxPathMatches, char[] path, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = CharArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = CharArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addPrune(nextOffset, offset);
						
						break;
					}
					default: {
						offset = CharArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addPrune(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
				maxPathMatches--;
				if(maxPathMatches == 0) {
					break; // done filtering
				}

			} else {
				offset = nextOffset + 1;
			}
		}
	}	
	
	public static void rangesPruneAnyPath(final byte[] chars, int offset, int limit, int maxPathMatches, byte[] path, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addPrune(nextOffset, offset);
						
						break;
					}
					default: {
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addPrune(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
				maxPathMatches--;
				if(maxPathMatches == 0) {
					break; // done filtering
				}

			} else {
				offset = nextOffset + 1;
			}
		}
	}

	public static void rangesAnonymizeAnyPath(final byte[] chars, int offset, int limit, int maxPathMatches, byte[] path, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			int quoteIndex = nextOffset;
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			if(matchPath(chars, offset + 1, quoteIndex, path)) {
				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);

				switch(chars[nextOffset]) {
					case '[':
					case '{':
						offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
						break;
					case '"': {
						offset = nextOffset;
						while(chars[++offset] != '"');
						if(chars[offset - 1] == '\\') {
							offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
						}
						offset++;
						filter.addAnon(nextOffset, offset);
						
						break;
					}
					default: {
						offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
						filter.addAnon(nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;		
					}

				}
				maxPathMatches--;
				if(maxPathMatches == 0) {
					break; // done filtering
				}

			} else {
				offset = nextOffset + 1;
			}
		}
	}
	

	public static void rangesPruneAnyPath(final byte[] chars, int offset, int limit, int maxPathMatches, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addPrune(nextOffset, offset);
					
					break;
				}
				default: {
					offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addPrune(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}

			}
			
			maxPathMatches--;
			if(maxPathMatches == 0) {
				break; // done filtering
			}
			
		}
	}
	
	public static void rangesAnonymizeAnyPath(final byte[] chars, int offset, int limit, int maxPathMatches, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
			int nextOffset = offset;
			while(chars[++nextOffset] != '"');
			if(chars[nextOffset - 1] == '\\') {
				nextOffset = ByteArrayRangesFilter.scanEscapedValue(chars, nextOffset);
			}
			
			while(chars[++nextOffset] <= 0x20);
			
			// is this a field name or a value? A field name must be followed by a colon
			if(chars[nextOffset] != ':') {
				// skip over whitespace
				
				// optimization: scan for highest value
				// space: 0x20
				// tab: 0x09
				// carriage return: 0x0D
				// newline: 0x0A

				// was a text value
				offset = nextOffset;
				
				continue;
			}
			
			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);

			switch(chars[nextOffset]) {
				case '[':
				case '{':
					offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
					break;
				case '"': {
					offset = nextOffset;
					while(chars[++offset] != '"');
					if(chars[offset - 1] == '\\') {
						offset = ByteArrayRangesFilter.scanEscapedValue(chars, offset);
					}
					offset++;
					filter.addAnon(nextOffset, offset);
					
					continue;
				}
				default: {
					offset = ByteArrayRangesFilter.scanBeyondUnquotedValue(chars, nextOffset);
					filter.addAnon(nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;		
				}

			}
			
			maxPathMatches--;
			if(maxPathMatches == 0) {
				break; // done filtering
			}
		}
	}	
	
}
