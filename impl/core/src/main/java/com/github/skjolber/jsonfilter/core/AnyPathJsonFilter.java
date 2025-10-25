package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilters;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

public class AnyPathJsonFilter extends AbstractRangesMultiPathJsonFilter {

	public AnyPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(-1, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	public AnyPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(-1, -1, maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}
	
	protected AnyPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(anyPathFilters == null) {
			throw new IllegalArgumentException("Expected at least one anonymize or prune filter");
		}
	}

	@Override
	public CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		final CharArrayRangesFilter filter = getCharArrayRangesFilter(maxPathMatches, length);
		try {
			AnyPathFilters anyElementFilters = this.anyPathFilters;
			if(maxPathMatches != -1) {
				rangesAnyPath(chars, offset, offset + length, maxPathMatches, anyElementFilters, filter);
			} else {
				rangesAnyPath(chars, offset, offset + length, anyElementFilters, filter);
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
			AnyPathFilters anyElementFilters = this.anyPathFilters;
			if(maxPathMatches != -1) {
				rangesAnyPath(chars, offset, offset + length, maxPathMatches, anyElementFilters, filter);
			} else {
				rangesAnyPath(chars, offset, offset + length, anyElementFilters, filter);
			}
			return filter;
		} catch(Exception e) {
			return null;
		}
	}

	public static void rangesAnyPath(final char[] chars, int offset, int limit, int pathMatches, AnyPathFilters anyElementFilters, CharArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
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

			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);
			
			FilterType filterType = anyElementFilters.matchPath(chars, offset + 1, quoteIndex);
			
			if(filterType != null) {
				switch(chars[nextOffset]) {
					case '[':
					case '{':
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset));
						} else {
							offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset, filter);
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
						// next char cannot be quote, so skip it
						offset++;
						break;
					}
					case 'f': {
						offset = nextOffset + 5;
						filter.add(filterType, nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;
						break;
					}
					default: {
						offset = nextOffset;
						loop:
						while(true) {
							switch(chars[++offset]) {
							case '0':
							case '1':
							case '2':
							case '3':
							case '4':
							case '5':
							case '6':
							case '7':
							case '8':
							case '9':
							case '-':
							case '+':
							case 'e':
							case 'E':
								continue;
							default: break loop;
						}
						}
						filter.add(filterType, nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;
					}
				}
				
				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						return; // done filtering
					}
				}					
			} else {
				offset = nextOffset;
			}
		}
	}

	public static void rangesAnyPath(final byte[] chars, int offset, int limit, int pathMatches, AnyPathFilters anyElementFilters, ByteArrayRangesFilter filter) {
		while(offset < limit) {
			if(chars[offset] != '"') {
				offset++;
				continue;
			}
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

			// skip whitespace after colon
			while(chars[++nextOffset] <= 0x20);
			
			FilterType filterType = anyElementFilters.matchPath(chars, offset + 1, quoteIndex);
			if(filterType != null) {
				switch(chars[nextOffset]) {
				case '[':
				case '{':
					if(filterType == FilterType.PRUNE) {
						filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset));
					} else {
						offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset, filter);
					}
					break;
				case '"': {
					offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
					filter.add(filterType, nextOffset, offset);
					break;
				}
				case 't': 
				case 'n': {
					offset = nextOffset + 4;
					filter.add(filterType, nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
					break;
				}
				case 'f': {
					offset = nextOffset + 5;
					filter.add(filterType, nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
					break;
				}
				default: {
					offset = nextOffset;
					loop:
					while(true) {
						switch(chars[++offset]) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
						case '-':
						case '+':
						case 'e':
						case 'E':
							continue;
						default: break loop;
					}
					}
					filter.add(filterType, nextOffset, offset);
					// next char cannot be quote, so skip it
					offset++;
				}
			}

				if(pathMatches != -1) {
					pathMatches--;
					if(pathMatches == 0) {
						return; // done filtering
					}
				}					
			} else {
				offset = nextOffset;
			}
		}
	}

	public static void rangesAnyPath(final char[] chars, int offset, int limit, AnyPathFilters anyElementFilters, CharArrayRangesFilter filter) {
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

				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);
				
				FilterType filterType = anyElementFilters.matchPath(chars, offset + 1, quoteIndex);
				
				if(filterType != null) {
					switch(chars[nextOffset]) {
					case '[':
					case '{':
						if(filterType == FilterType.PRUNE) {
							filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset));
						} else {
							offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset, filter);
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
						// next char cannot be quote, so skip it
						offset++;
						break;
					}
					case 'f': {
						offset = nextOffset + 5;
						filter.add(filterType, nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;
						break;
					}
					default: {
						offset = nextOffset;
						loop:
						while(true) {
							switch(chars[++offset]) {
							case '0':
							case '1':
							case '2':
							case '3':
							case '4':
							case '5':
							case '6':
							case '7':
							case '8':
							case '9':
							case '-':
							case '+':
							case 'e':
							case 'E':
								continue;
							default: break loop;
						}
						}
						filter.add(filterType, nextOffset, offset);
						// next char cannot be quote, so skip it
						offset++;
					}
				}

				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}
	}

	public static void rangesAnyPath(final byte[] chars, int offset, int limit, AnyPathFilters anyElementFilters, ByteArrayRangesFilter filter) {
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

				// skip whitespace after colon
				while(chars[++nextOffset] <= 0x20);
				
				FilterType filterType = anyElementFilters.matchPath(chars, offset + 1, quoteIndex);
				if(filterType != null) {
					switch(chars[nextOffset]) {
						case '[':
						case '{':
							if(filterType == FilterType.PRUNE) {
								filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset));
							} else {
								offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset, filter);
							}
							break;
						case '"': {
							offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
							filter.add(filterType, nextOffset, offset);
							break;
						}
						case 't': 
						case 'n': {
							offset = nextOffset + 4;
							filter.add(filterType, nextOffset, offset);
							// next char cannot be quote, so skip it
							offset++;
							break;
						}
						case 'f': {
							offset = nextOffset + 5;
							filter.add(filterType, nextOffset, offset);
							// next char cannot be quote, so skip it
							offset++;
							break;
						}
						default: {
							offset = nextOffset;
							loop:
							while(true) {
								switch(chars[++offset]) {
								case '0':
								case '1':
								case '2':
								case '3':
								case '4':
								case '5':
								case '6':
								case '7':
								case '8':
								case '9':
								case '-':
								case '+':
								case 'e':
								case 'E':
									continue;
								default: break loop;
							}
							}
							filter.add(filterType, nextOffset, offset);
							// next char cannot be quote, so skip it
							offset++;
						}
					}
				
				} else {
					offset = nextOffset;
				}
				continue;
			}
			offset++;
		}
	}

}
