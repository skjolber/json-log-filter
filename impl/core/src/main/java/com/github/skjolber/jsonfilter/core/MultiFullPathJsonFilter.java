package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.base.AbstractMultiCharArrayPathFilter;
import com.github.skjolber.jsonfilter.base.CharArrayFilter;

public class MultiFullPathJsonFilter extends AbstractMultiCharArrayPathFilter {

	public MultiFullPathJsonFilter(int maxPathMatches, String[] anonymizes, String[] prunes) {
		super(-1, maxPathMatches, anonymizes, prunes);
		
		if(anyElementFilters != null) {
			throw new IllegalArgumentException("Expected no any-element searches (i.e. '//myField')");
		}
	}

	@Override
	public CharArrayFilter ranges(final char[] chars, int offset, int length) {
		int pathMatches = this.maxPathMatches;

		final int[] elementFilterStart = this.elementFilterStart;

		final int[] elementMatches = new int[elementFilters.length];

		length += offset;

		int level = 0;
		
		CharArrayFilter filter = new CharArrayFilter();

		try {
			main:
			while(offset < length) {
				switch(chars[offset]) {
					case '{' :
						level++;
						
						if(level > elementFilterStart.length) {
							offset = CharArrayFilter.skipObject(chars, offset);
							
							level--;
									
							continue;
						}
						break;
					case '}' :
						level--;
						
						if(level < elementFilterStart.length) {
							constrainMatches(elementMatches, level);
						}
						
						break;
					case '"' : { 
						if(level >= elementFilterStart.length) {
							// not necessary to check if field or value; missing sub-path
							// so if this is a key, there will never be a full match
							offset = CharArrayFilter.scanBeyondQuotedValue(chars, offset);
							
							continue;
						}
						int nextOffset = CharArrayFilter.scanBeyondQuotedValue(chars, offset);

						// is this a field name or a value? A field name must be followed by a colon
						int mark = nextOffset - 1;
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

						// match again any higher filter
						if(level < elementFilterStart.length && matchElements(chars, offset + 1, mark, level, elementMatches)) {
							for(int i = elementFilterStart[level]; i < elementFilterEnd[level]; i++) {
								if(elementMatches[i] == level) {
									// matched
									if(elementFilters[i].filterType == FilterType.PRUNE) {
										// skip whitespace. Strictly not necessary, but produces expected results for pretty-printed documents
										while(chars[nextOffset] <= 0x20) { // expecting colon, comma, end array or end object
											nextOffset++;
										}
										filter.add(nextOffset, offset = CharArrayFilter.skipSubtree(chars, nextOffset), FilterType.PRUNE.getType());
									} else {
										// special case: anon scalar values
										if(chars[nextOffset] == '"') {
											// quoted value
											offset = CharArrayFilter.scanBeyondQuotedValue(chars, nextOffset);
											
											filter.addAnon(nextOffset, offset);
										} else if(chars[nextOffset] == 't' || chars[nextOffset] == 'f' || (chars[nextOffset] >= '0' && chars[nextOffset] <= '9') || chars[nextOffset] == '-') {
											// scalar value
											offset = CharArrayFilter.scanBeyondUnquotedValue(chars, nextOffset);

											filter.addAnon(nextOffset, offset);
										} else {
											// filter as tree
											offset = CharArrayFilter.anonymizeSubtree(chars, nextOffset, filter);
										}
										
									}
									
									pathMatches--;
									if(pathMatches <= 0) {
										break main; // done filtering
									}
									
									constrainMatches(elementMatches, level - 1);
									
									continue main;
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

			if(offset > length) { // so checking bounds here; one of the scan methods might have overshoot due to corrupt JSON. 
				return null;
			}

			return filter;
		} catch(Exception e) {
			return null;
		}		
	}

	
}
