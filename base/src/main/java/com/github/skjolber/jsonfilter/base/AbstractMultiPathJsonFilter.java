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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractMultiPathJsonFilter extends AbstractPathJsonFilter {

	public static class AbsolutePathFilter {
		
		public final char[][] paths;
		public final String[] pathStrings;
		public final FilterType filterType;
		
		public AbsolutePathFilter(String[] pathStrings, FilterType filterType) {
			this.paths = toCharArray(pathStrings);
			this.pathStrings = pathStrings;
			this.filterType = filterType;
		}
		
		protected int getLength() {
			return paths.length;
		}
		
		protected FilterType getFilterType() {
			return filterType;
		}
		
	}
	
	public static class AnyPathFilter {
		
		public final String pathString;
		public final char[] path;
		public final FilterType filterType;
		
		public AnyPathFilter(String pathString, FilterType filterType) {
			this.pathString = pathString;
			this.path = intern(pathString.toCharArray());
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
	protected final int[] elementFilterEnd;

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
			
			// count
			for(int i = 0; i < elements.size(); i++) {
				if(elementFilterEnd[elements.get(i).getLength()] == 0) { // first filter for this index
					elementFilterStart[elements.get(i).getLength()] = i;
				}
				elementFilterEnd[elements.get(i).getLength()]++;
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
	
	protected boolean matchAnon(int[] matches, int level) {
		for(int i = elementFilterStart[level]; i < matches.length; i++) {
			if(matches[i] == elementFilters[i].getLength() && elementFilters[i].getFilterType() == FilterType.ANON) {
				return true;
			}
		}
		return false;
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
				
				if(elementMatches[i] >= elementFilters[i].paths.length) {
					// this filter is at the maximum
					continue;
				}

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
}
