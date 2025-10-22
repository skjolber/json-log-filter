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
	protected final AnyPathFilter[][] anyElementFiltersBytes; // outer array: key length in bytes
	protected final AnyPathFilter[][] anyElementFiltersChars; // outer array: key length in chars

	public AbstractMultiPathJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		List<String> pathsList = new ArrayList<>();
		List<FilterType> typesList = new ArrayList<>();

		List<AnyPathFilter> any = new ArrayList<>(); // prunes take precedence of anonymizes

		if(prunes != null) {
			for(int i = 0; i < prunes.length; i++) {
				String prune = prunes[i];
				if(hasAnyPrefix(prune)) {
					String name = prune.substring(2);
					if(name.equals("*")) {
						throw new IllegalArgumentException("Unexpected any match for *");
					}
					any.add(new AnyPathFilter(name, FilterType.PRUNE));
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
					String name = anonymize.substring(2);
					if(name.equals("*")) {
						throw new IllegalArgumentException("Unexpected any match for *");
					}
					any.add(new AnyPathFilter(name, FilterType.ANON));
				} else {
					pathsList.add(anonymize);
					typesList.add(FilterType.ANON);
				}
			}
		}
		
		if(!any.isEmpty()) {
			int maxKeyLengthBytes = 0;
			int maxKeyLengthChars = 0;
			for(int i = 0; i < any.size(); i++) {
				AnyPathFilter anyPathFilter = any.get(i);
				if(maxKeyLengthBytes < anyPathFilter.pathBytes.length) {
					maxKeyLengthBytes = anyPathFilter.pathBytes.length;
				}
				if(maxKeyLengthChars < anyPathFilter.pathChars.length) {
					maxKeyLengthChars = anyPathFilter.pathChars.length;
				}
			}
			
			anyElementFiltersBytes = fillBytes(any, maxKeyLengthBytes);
			anyElementFiltersChars = fillChars(any, maxKeyLengthChars);
		} else {
			anyElementFiltersBytes = null;
			anyElementFiltersChars = null;
		}

		ExpressionNode expressionNode = NODE_FACTORY.toExpressionNode(pathsList, typesList);
		
		this.pathItem = FACTORY.create(expressionNode);		
	}
	
	private AnyPathFilter[][] fillBytes(List<AnyPathFilter> any, int length) {
		List<AnyPathFilter>[] output = new List[length+1];
		for(int i = 0; i < output.length; i++) {
			output[i] = new ArrayList<>();
		}
		
		for(AnyPathFilter filter : any) {
			output[filter.pathBytes.length].add(filter);
		}
		
		AnyPathFilter[][] result = new AnyPathFilter[length + 1][];
		for(int i = 0; i < result.length; i++) {
			if(!output[i].isEmpty()) {
				result[i] = output[i].toArray(new AnyPathFilter[output[i].size()]);
			}
		}
	
		 return result;
	}
	
	private AnyPathFilter[][] fillChars(List<AnyPathFilter> any, int length) {
		List<AnyPathFilter>[] output = new List[length+1];
		for(int i = 0; i < output.length; i++) {
			output[i] = new ArrayList<>();
		}
		
		for(AnyPathFilter filter : any) {
			output[filter.pathChars.length].add(filter);
		}
		
		AnyPathFilter[][] result = new AnyPathFilter[length + 1][];
		for(int i = 0; i < result.length; i++) {
			if(!output[i].isEmpty()) {
				result[i] = output[i].toArray(new AnyPathFilter[output[i].size()]);
			}
		}
	
		 return result;
	}
	
	
	/**
	 * Note that the order or the filters establishes precedence (prune over anon).
	 * 
	 * @param chars JSON characters
	 * @return the matching filter type, or null if none
	 */
	
	protected FilterType matchAnyElements(final String chars) {
		
		if(chars.length() >= anyElementFiltersChars.length || anyElementFiltersChars[chars.length()] == null) {
			return null;
		}
		AnyPathFilter[] anyPathFilters = anyElementFiltersChars[chars.length()];
		
		for(int i = 0; i < anyPathFilters.length; i++) {
			if(anyPathFilters[i].pathString.equals(chars)) {
				return anyPathFilters[i].getFilterType();
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
		return matchAnyElements(anyElementFiltersChars, chars, start, end);
	}
	
	protected static FilterType matchAnyElements(AnyPathFilter[][] anyElementFiltersChars, final char[] chars, int start, int end) {
		int length = end - start;
		if(length >= anyElementFiltersChars.length || anyElementFiltersChars[length] == null) {
			return null;
		}
		return matchAnyElements(anyElementFiltersChars[length], chars, start, end);
	}

	protected static FilterType matchAnyElements(AnyPathFilter[] anyElementFilters, final char[] chars, int start, int end) {
		for(int i = 0; i < anyElementFilters.length; i++) {
			if(AbstractPathJsonFilter.matchPath(chars, start, end, anyElementFilters[i].pathChars)) {
				return anyElementFilters[i].getFilterType();
			}
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
		return matchAnyElements(anyElementFiltersBytes, chars, start, end);
	}
	
	protected static FilterType matchAnyElements(AnyPathFilter[][] anyElementFiltersBytes, final byte[] chars, int start, int end) {
		int length = end - start;
		if(length < anyElementFiltersBytes.length && anyElementFiltersBytes[length] != null) {
			FilterType type = unencodedMatchAnyElements(anyElementFiltersBytes[length], chars, start, end);
			if(type != null) {
				return type;
			}
		}
		
		
		
		return matchAnyElements(anyElementFiltersBytes[length], chars, start, end);
	}	
	
	protected static FilterType matchAnyElements(AnyPathFilter[] anyElementFilters, final byte[] chars, int start, int end) {
		for(int i = 0; i < anyElementFilters.length; i++) {
			if(AbstractPathJsonFilter.matchPath(chars, start, end, anyElementFilters[i].pathBytes)) {
				return anyElementFilters[i].getFilterType();
			}
		}
		return null;
	}	

	protected static FilterType unencodedMatchAnyElements(AnyPathFilter[] anyElementFilters, final byte[] chars, int start, int end) {
		
		main:
		for(int i = 0; i < anyElementFilters.length; i++) {
			byte[] pathBytes = anyElementFilters[i].pathBytes;
			for(int k = 0; i < pathBytes.length; i++) {
				if(pathBytes[k] != chars[start + k]) {
					continue main;
				}
			}
		}
		return null;
	}	

}
