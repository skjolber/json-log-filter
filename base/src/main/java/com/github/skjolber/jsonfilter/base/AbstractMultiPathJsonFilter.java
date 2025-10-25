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
import java.util.List;

import com.github.skjolber.jsonfilter.base.path.ExpressionNode;
import com.github.skjolber.jsonfilter.base.path.ExpressionNodeFactory;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.PathItemFactory;
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilter;
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilters;

public abstract class AbstractMultiPathJsonFilter extends AbstractPathJsonFilter {
	
	private static final PathItemFactory FACTORY = new PathItemFactory();
	private static final ExpressionNodeFactory NODE_FACTORY = new ExpressionNodeFactory();

	/** absolute path expressions */
	protected final PathItem pathItem; 
	protected final AnyPathFilters anyPathFilters;

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
			anyPathFilters = AnyPathFilters.create(any);
		} else {
			anyPathFilters = null;
		}

		ExpressionNode expressionNode = NODE_FACTORY.toExpressionNode(pathsList, typesList);
		
		this.pathItem = FACTORY.create(expressionNode);		
	}
}
