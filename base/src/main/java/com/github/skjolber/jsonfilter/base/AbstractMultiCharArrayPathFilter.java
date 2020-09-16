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

public abstract class AbstractMultiCharArrayPathFilter extends AbstractMultiPathJsonFilter {

	public AbstractMultiCharArrayPathFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
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
				if(matchPath(chars, start, end, elementFilters[i].paths[elementMatches[i]])) {
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
				if(matchPath(chars, start, end, elementFilters[i].paths[elementMatches[i]])) {
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
			if(anyElementFilters[i].path.length != end - start) {
				continue;
			}
			for(int k = 0; k < anyElementFilters[i].path.length; k++) {
				if(anyElementFilters[i].path[k] != chars[start + k]) {
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
			if(anyElementFilters[i].path.length != end - start) {
				continue;
			}
			for(int k = 0; k < anyElementFilters[i].path.length; k++) {
				if(anyElementFilters[i].path[k] != chars[start + k]) {
					continue anyFilters;
				}
			}
			
			return anyElementFilters[i].getFilterType();
		}
		return null;
			
	}
}
