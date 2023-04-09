package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.match.PathItem;
import com.github.skjolber.jsonfilter.base.match.PathItemFactory;

public class MultiFullPathJsonFilter extends AbstractRangesMultiPathJsonFilter {

	private final PathItem pathItem; 
	
	public MultiFullPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(-1, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		if(anyElementFilters != null) {
			throw new IllegalArgumentException("Expected no any-element searches (i.e. '//myField')");
		}
		
		int count = 0;
		if(anonymizes != null) {
			count += anonymizes.length;
		}
		if(prunes != null) {
			count += prunes.length;
		}
		
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[count];
		String[] expressions = new String[count];

		int index = 0;
		if(anonymizes != null) {
			for(int i = 0; i < anonymizes.length; i++) {
				expressions[i] = anonymizes[i];
				types[i] = FilterType.ANON;
			}
		}
		if(prunes != null) {
			for(int i = 0; i < prunes.length; i++) {
				expressions[i + index] = prunes[i];
				types[i + index] = FilterType.PRUNE;
			}
		}

		this.pathItem = factory.create(expressions, types);
	}
	
	public MultiFullPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		this(maxPathMatches, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	@Override
	protected CharArrayRangesFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final CharArrayRangesFilter filter = getCharArrayRangesFilter(pathMatches, length);

		length += offset;

		int level = 0;
		
		PathItem pathItem = this.pathItem;
		
		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level >= elementFilterStart.length) {
							// so other always level < elementFilterStart.length
							offset = CharArrayRangesFilter.skipObject(chars, offset + 1);
							
							level--;
									
							continue;
						}
						System.out.println("Start level " + level);
						break;
					case '}' :
						System.out.println("End level " + level);
						
						level--;
						
						pathItem = pathItem.constrain(level);
						
						break;
					case '"' : { 
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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

						// match again any higher filter
						pathItem = pathItem.matchPath(chars, offset + 1, quoteIndex);

						if(pathItem.hasType()) {
							// matched
							FilterType type = pathItem.getType();
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = CharArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
								} else {
									offset = CharArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
								}
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = CharArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = CharArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								if(type == FilterType.PRUNE) {
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
							pathItem = pathItem.constrain(level);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(level != 0) {
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}		
	}
	

	@Override
	protected ByteArrayRangesFilter ranges(final byte[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;

		final int[] elementMatches = new int[elementFilters.length];

		length += offset;

		int level = 0;
		
		final ByteArrayRangesFilter filter = getByteArrayRangesFilter(pathMatches);

		try {
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level > elementFilterStart.length) {
							offset = ByteArrayRangesFilter.skipObject(chars, offset + 1);
							
							level--;
									
							continue;
						}
						System.out.println("Start level " + level);
						break;
					case '}' :
						System.out.println("End level " + level);
						level--;
						
						constrainMatches(elementMatches, level);
						
						break;
					case '"' : { 
						if(level >= elementFilterStart.length) {
							// not necessary to check if field or value; missing sub-path
							// so if this is a key, there will never be a full match
							do {
								offset++;
							} while(chars[offset] != '"' || chars[offset - 1] == '\\');
							offset++;							
							
							continue;
						}
						
						int nextOffset = offset;
						do {
							nextOffset++;
						} while(chars[nextOffset] != '"' || chars[nextOffset - 1] == '\\');
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

						// match again any higher filter
						FilterType type = matchElements(chars, offset + 1, quoteIndex, level, elementMatches);
						if(type != null) {
							// matched
							if(chars[nextOffset] == '[' || chars[nextOffset] == '{') {
								if(type == FilterType.PRUNE) {
									filter.addPrune(nextOffset, offset = ByteArrayRangesFilter.skipObjectOrArray(chars, nextOffset + 1));
								} else {
									offset = ByteArrayRangesFilter.anonymizeObjectOrArray(chars, nextOffset + 1, filter);
								}
							} else {
								if(chars[nextOffset] == '"') {
									// quoted value
									offset = ByteArrayRangesFilter.scanBeyondQuotedValue(chars, nextOffset);
								} else {
									offset = ByteArrayRangesFilter.scanUnquotedValue(chars, nextOffset);
								}
								if(type == FilterType.PRUNE) {
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
							
							System.out.println("Constrain match");
							constrainMatches(elementMatches, level);
						} else {
							offset = nextOffset;
						}
						
						continue;
					}
					default : 
				}
				offset++;
			}

			if(level != 0) {
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}		
	}
		
}
