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
import java.util.List;

import com.github.skjolber.jsonfilter.base.path.ExpressionNode;
import com.github.skjolber.jsonfilter.base.path.ExpressionNodeFactory;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.PathItemFactory;

public abstract class AbstractMultiPathJsonFilter extends AbstractPathJsonFilter {
	
	private static final PathItemFactory FACTORY = new PathItemFactory();
	private static final ExpressionNodeFactory NODE_FACTORY = new ExpressionNodeFactory();

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
	
	/** absolute path expressions */
	protected final PathItem pathItem; 
	
	/** any path expression - //element */
	protected final AnyPathFilter[] anyElementFilters;

	public AbstractMultiPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		List<String> pathsList = new ArrayList<>();
		List<FilterType> typesList = new ArrayList<>();

		List<AnyPathFilter> any = new ArrayList<>(); // prunes take precedence of anonymizes

		if(prunes != null) {
			for(int i = 0; i < prunes.length; i++) {
				String prune = prunes[i];
				if(hasAnyPrefix(prune)) {
					any.add(new AnyPathFilter(prune.substring(2), FilterType.PRUNE));
				} else {
					pathsList.add(prune);
					typesList.add(FilterType.PRUNE);
				}
			}
		}

		if(anonymizes != null) {
			for(int i = 0; i < anonymizes.length; i++) {
				String anonymize = anonymizes[i];
				if(hasAnyPrefix(anonymize)) {
					any.add(new AnyPathFilter(anonymize.substring(2), FilterType.ANON));
				} else {
					pathsList.add(anonymize);
					typesList.add(FilterType.ANON);
				}
			}
		}
		
		if(!any.isEmpty()) {
			anyElementFilters = any.toArray(new AnyPathFilter[any.size()]);
		} else {
			anyElementFilters = null;
		}

		ExpressionNode expressionNode = NODE_FACTORY.toExpressionNode(pathsList, typesList);
		
		this.pathItem = FACTORY.create(expressionNode);		
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
