/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.skjolber.jsonfilter.base;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractMultiPathJsonFilter extends AbstractPathJsonFilter {

	protected static class AbsolutePathFilter {
		
		public final byte[][] pathBytes;
		public final char[][] pathChars;
		public final String[] pathStrings;
		public final FilterType filterType;
		
		public AbsolutePathFilter(String[] pathStrings, FilterType filterType) {
			this.pathChars = toCharArray(pathStrings);
			this.pathBytes = toByteArray(pathStrings);
			this.pathStrings = pathStrings;
			this.filterType = filterType;
		}
		
		protected int getLength() {
			return pathChars.length;
		}
		
	}
	
	public static class AnyPathFilter {
		
		public final String pathString;
		public final char[] pathChars;
		public final byte[] pathBytes;
		
		public final FilterType filterType;
		
		public AnyPathFilter(String pathString, FilterType filterType) {
			this.pathString = pathString;
			this.pathChars = intern(pathString.toCharArray());
			this.pathBytes = intern(pathString.getBytes(StandardCharsets.UTF_8));
			this.filterType = filterType;
		}

		protected FilterType getFilterType() {
			return filterType;
		}
	}

	protected static final Comparator<AbsolutePathFilter> comparator = (AbsolutePathFilter o1, AbsolutePathFilter o2) -> Integer.compare(o1.getLength(), o2.getLength());
	
	/** absolute path expressions */
	protected final AbsolutePathFilter[] elementFilters;

	/** any path expression - //element */
	protected final AnyPathFilter[] anyElementFilters;
	
	protected final int[] elementFilterStart;
	protected final int[] elementFilterEnd; // exclusive

	public AbstractMultiPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		List<AbsolutePathFilter> elements = new ArrayList<>();

		List<AnyPathFilter> any = new ArrayList<>(); // prunes take precedence of anonymizes

		if(prunes != null) {
			for(int i = 0; i < prunes.length; i++) {
				String prune = prunes[i];
				if(hasAnyPrefix(prune)) {
					any.add(new AnyPathFilter(prune.substring(2), FilterType.PRUNE));
				} else {
					elements.add(new AbsolutePathFilter(parse(prune), FilterType.PRUNE));
				}
			}
		}

		if(anonymizes != null) {
			for(int i = 0; i < anonymizes.length; i++) {
				String anonymize = anonymizes[i];
				if(hasAnyPrefix(anonymize)) {
					any.add(new AnyPathFilter(anonymize.substring(2), FilterType.ANON));
				} else {
					elements.add(new AbsolutePathFilter(parse(anonymize), FilterType.ANON));
				}
			}
		}
		
		if(!any.isEmpty()) {
			anyElementFilters = any.toArray(new AnyPathFilter[any.size()]);
		} else {
			anyElementFilters = null;
		}
		
		if(!elements.isEmpty()) {
			Collections.sort(elements, comparator);
			
			int maxElementPaths = Integer.MIN_VALUE;
			for(AbsolutePathFilter elementPath : elements) {
				if(elementPath.getLength() > maxElementPaths) {
					maxElementPaths = elementPath.getLength();
				}
			}
			
			elementFilterStart = new int[maxElementPaths + 1];
			elementFilterEnd = new int[maxElementPaths + 1];

			for (AbsolutePathFilter absolutePathFilter : elements) {
				elementFilterEnd[absolutePathFilter.getLength()]++;
			}
			
			for(int i = 1; i < elementFilterEnd.length; i++) {
				int sum = 0;
				for(int k = 0; k < i; k++) {
					sum += elementFilterEnd[k];
				}
				
				elementFilterStart[i] = sum;
			}

			// add start to count for end
			for(int i = 0; i < elementFilterEnd.length; i++) {
				elementFilterEnd[i] += elementFilterStart[i];
			}

			elementFilters = elements.toArray(new AbsolutePathFilter[elements.size()]);
		} else {
			elementFilterStart = new int[]{};
			elementFilterEnd = new int[]{};
			elementFilters = new AbsolutePathFilter[]{};
		}

	}
	
	protected static void constrain(int[] filter, int[] matches, int level) {
		for(int i = filter[level]; i < matches.length; i++) {
			if(matches[i] > level) {
				matches[i] = level;
			}
		}
	}

	protected void constrainMatchesCheckLevel(int[] matches, int level) {
		if(level < elementFilterStart.length) {
			constrain(elementFilterStart, matches, level);
		}
	}

	protected void constrainMatches(int[] matches, int level) {
		constrain(elementFilterStart, matches, level);
	}
	
	protected boolean matchElements(final String chars, int level, final int[] elementMatches) {
		boolean match = false;
		for(int i = elementFilterStart[level]; i < elementMatches.length; i++) {
			if(elementMatches[i] == level - 1) {
				if(matchPath(chars, elementFilters[i].pathStrings[elementMatches[i]])) {
					elementMatches[i]++;
					if(i < elementFilterEnd[level]) {
						match = true;
					}
				}

			}
		}
		return match;
	}
	
	/**
	 * Note that the order or the filters establishes precedence (prune over anon).
	 * 
	 * @param chars XML characters
	 * @return the matching filter type, or null if none
	 */
	
	protected FilterType matchAnyElements(final String chars) {
		for(int i = 0; i < anyElementFilters.length; i++) {
			if(anyElementFilters[i].pathString.equals(chars)) {
				return anyElementFilters[i].getFilterType();
			}
		}
		return null;
			
	}
	
	/**
	 * Match a char range against a specific level of path expressions.
	 * 
	 * @param chars text source
	 * @param start text source start
	 * @param end text source end (exclusive)
	 * @param level path level
	 * @param elementMatches current expression matches, constrained to the current level minus one
	 * @return true if all segments of a path expression is matched
	 */

	protected boolean matchElements(final char[] chars, int start, int end, int level, final int[] elementMatches) {
		boolean match = false;
		
		for(int i = elementFilterStart[level]; i < elementMatches.length; i++) {
			if(elementMatches[i] == level - 1) {
				if(matchPath(chars, start, end, elementFilters[i].pathChars[elementMatches[i]])) {
					elementMatches[i]++;
					
					if(i < elementFilterEnd[level]) {
						match = true;
					}
				}

			}
		}
		return match;
	}

	/**
	 * Match a byte range against a specific level of path expressions.
	 * 
	 * @param chars text source
	 * @param start text source start
	 * @param end text source end (exclusive)
	 * @param level path level
	 * @param elementMatches current expression matches, constrained to the current level minus one
	 * @return true if all segments of a path expression is matched
	 */

	protected boolean matchElements(final byte[] chars, int start, int end, int level, final int[] elementMatches) {
		boolean match = false;
		
		for(int i = elementFilterStart[level]; i < elementMatches.length; i++) {
			if(elementMatches[i] == level - 1) {
				if(matchPath(chars, start, end, elementFilters[i].pathBytes[elementMatches[i]])) {
					elementMatches[i]++;
					
					if(i < elementFilterEnd[level]) {
						match = true;
					}
				}

			}
		}
		return match;
	}	
	
	/**
	 * Match a char range against any-type expressions.
	 * 
	 * Note that the order or the filters establishes precedence (prune over anon).
	 * 
	 * @param chars JSON characters
	 * @param start JSON characters start position
	 * @param end JSON characters end position
	 * @return the matching filter type, or null if none
	 */
	
	protected FilterType matchAnyElements(final char[] chars, int start, int end) {
		anyFilters:
		for(int i = 0; i < anyElementFilters.length; i++) {
			if(anyElementFilters[i].pathChars.length != end - start) {
				continue;
			}
			for(int k = 0; k < anyElementFilters[i].pathChars.length; k++) {
				if(anyElementFilters[i].pathChars[k] != chars[start + k]) {
					continue anyFilters;
				}
			}
			
			return anyElementFilters[i].getFilterType();
		}
		return null;
			
	}

	/**
	 * Match a byte range against any-type expressions.
	 * 
	 * Note that the order or the filters establishes precedence (prune over anon).
	 * 
	 * @param chars JSON characters
	 * @param start JSON characters start position
	 * @param end JSON characters end position
	 * @return the matching filter type, or null if none
	 */

	protected FilterType matchAnyElements(final byte[] chars, int start, int end) {
		anyFilters:
		for(int i = 0; i < anyElementFilters.length; i++) {
			if(anyElementFilters[i].pathBytes.length != end - start) {
				continue;
			}
			for(int k = 0; k < anyElementFilters[i].pathBytes.length; k++) {
				if(anyElementFilters[i].pathBytes[k] != chars[start + k]) {
					continue anyFilters;
				}
			}
			
			return anyElementFilters[i].getFilterType();
		}
		return null;
			
	}	
}
