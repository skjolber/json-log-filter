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
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

/**
 * 
 */

public class DefaultJsonFilterFactory extends AbstractJsonFilterFactory {
		
	/**
	 * Spawn a factory instance. Equivalent to using the default constructor.
	 * 
	 * @return newly created {@linkplain DefaultJsonFilterFactory}.
	 */
	
	public static DefaultJsonFilterFactory newInstance() {
		return new DefaultJsonFilterFactory();
	}
	
	/**
	 * Spawn a filter. 
	 * 
	 * @return new, or previously created, thread-safe pretty printer
	 */
	
	public JsonFilter newJsonFilter() {
		String pruneMessage = this.pruneMessage;
		if(pruneMessage == null) {
			pruneMessage = AbstractJsonFilter.FILTER_PRUNE_MESSAGE;
		}
		String anonymizeMessage = this.anonymizeMessage;
		if(anonymizeMessage == null) {
			anonymizeMessage = AbstractJsonFilter.FILTER_ANONYMIZE;
		}
		
		String truncateMessage = this.truncateMessage;
		if(truncateMessage == null) {
			truncateMessage = AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;
		}
		// check for any prune/anon filter
		if(isActivePathFilters()) {
			// check for single prune/anon filter
			if(isSinglePruneFilter()) {
				if(!pruneFilters[0].startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
					if(isActiveMaxStringLength()) {
						return new SingleFullPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, pruneFilters[0], FilterType.PRUNE, pruneMessage, anonymizeMessage, truncateMessage);
					} else {
						return new SingleFullPathJsonFilter(maxPathMatches, pruneFilters[0], FilterType.PRUNE, pruneMessage, anonymizeMessage, truncateMessage);
					}
				} else {
					if(isActiveMaxStringLength()) {
						return new SingleAnyPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, pruneFilters[0], FilterType.PRUNE, pruneMessage, anonymizeMessage, truncateMessage);
					} else {
						return new SingleAnyPathJsonFilter(maxPathMatches, pruneFilters[0], FilterType.PRUNE, pruneMessage, anonymizeMessage, truncateMessage);
					}
				}
			} else if(isSingleAnonymizeFilter()) {
				if(!anonymizeFilters[0].startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
					if(isActiveMaxStringLength()) {
						return new SingleFullPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, anonymizeFilters[0], FilterType.ANON, pruneMessage, anonymizeMessage, truncateMessage);
					} else {
						return new SingleFullPathJsonFilter(maxPathMatches, anonymizeFilters[0], FilterType.ANON, pruneMessage, anonymizeMessage, truncateMessage);
					}
				} else {
					if(isActiveMaxStringLength()) {
						return new SingleAnyPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, anonymizeFilters[0], FilterType.ANON, pruneMessage, anonymizeMessage, truncateMessage);
					} else {
						return new SingleAnyPathJsonFilter(maxPathMatches, anonymizeFilters[0], FilterType.ANON, pruneMessage, anonymizeMessage, truncateMessage);
					}
				}
			}
		
			if(isActiveMaxStringLength()) {
				return new MultiPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, anonymizeFilters, pruneFilters, pruneMessage, anonymizeMessage, truncateMessage);
			} else {
				if(!hasAnyPrefix(anonymizeFilters) && !hasAnyPrefix(pruneFilters)) {
					return new MultiFullPathJsonFilter(maxPathMatches, anonymizeFilters, pruneFilters, pruneMessage, anonymizeMessage, truncateMessage);
				}
				
				return new MultiPathJsonFilter(maxPathMatches, anonymizeFilters, pruneFilters, pruneMessage, anonymizeMessage, truncateMessage);
			}
		} else if(maxPathMatches != -1) {
			throw new IllegalArgumentException("If no prune or anonymize paths exists, max path matches should not be set.");
		}
		if(isActiveMaxStringLength()) {
			return new MaxStringLengthJsonFilter(maxStringLength);
		}

		return new DefaultJsonFilter();
	}


}
