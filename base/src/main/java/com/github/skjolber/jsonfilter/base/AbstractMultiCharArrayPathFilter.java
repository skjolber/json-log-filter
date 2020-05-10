/***************************************************************************
 * Copyright 2020 Thomas Rorvik Skjolberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

	public AbstractMultiCharArrayPathFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		super(maxStringLength, anonymizes, prunes);
	}

	protected boolean matchElements(final char[] chars, int start, int end, int level, final int[] elementMatches) {
		boolean match = false;
		
		for(int i = elementFilterStart[level]; i < elementMatches.length; i++) {
			if(elementMatches[i] == level - 1) {
				
				if(elementMatches[i] >= elementFilters[i].paths.length) {
					// this filter is at the maximum
					continue;
				}

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
	 * Note that the order or the filters establishes precedence (prune over anon).
	 * 
	 * @param chars JSON characters
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
