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
package com.github.skjolber.jsonfilter.core;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.MultiPathMaxStringLengthRemoveWhitespaceJsonFilter;
import com.github.skjolber.jsonfilter.core.ws.RemoveWhitespaceJsonFilter;

/**
 * Default {@linkplain JsonFilter} factory.
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
		String pruneJsonValue = this.pruneJsonValue;
		if(pruneJsonValue == null) {
			pruneJsonValue = AbstractJsonFilter.FILTER_PRUNE_MESSAGE_JSON;
		}
		String anonymizeJsonValue = this.anonymizeJsonValue;
		if(anonymizeJsonValue == null) {
			anonymizeJsonValue = AbstractJsonFilter.FILTER_ANONYMIZE_JSON;
		}
		
		String truncateStringValue = this.truncateStringValue;
		if(truncateStringValue == null) {
			truncateStringValue = AbstractJsonFilter.FILTER_TRUNCATE_MESSAGE;
		}
		
		String[] pruneFilters = this.pruneFilters.isEmpty() ? null : this.pruneFilters.toArray(new String[this.pruneFilters.size()]);
		String[] anonymizeFilters = this.anonymizeFilters.isEmpty() ? null :  this.anonymizeFilters.toArray(new String[this.anonymizeFilters.size()]);
		
		// check for any prune/anon filter
		if(isActivePathFilters()) {
			// check for single prune/anon filter
			
			if(removeWhitespace) {
				// TODO more filters for removing whitespace
				if(isActiveMaxSize()) {
					return new MultiPathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, maxSize, maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
				return new MultiPathMaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength, maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				
			} else {
				if(anonymizeFilters != null && AbstractPathJsonFilter.hasAllAnyPrefix(anonymizeFilters) && pruneFilters != null && AbstractPathJsonFilter.hasAllAnyPrefix(pruneFilters)) {
					if(isActiveMaxSize()) {
						return new AnyPathMaxSizeJsonFilter(maxSize, maxPathMatches, anonymizeFilters, pruneFilters);
					}
					return new AnyPathJsonFilter(maxPathMatches, anonymizeFilters, pruneFilters);
				}

				if(isActiveMaxStringLength()) {
					if(isActiveMaxSize()) {
						return new MultiPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
					}
					return new MultiPathMaxStringLengthJsonFilter(maxStringLength, maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				} else {
					if(!AbstractPathJsonFilter.hasAnyPrefix(anonymizeFilters) && !AbstractPathJsonFilter.hasAnyPrefix(pruneFilters)) {
						if(!isActiveMaxSize()) {
							return new MultiFullPathJsonFilter(maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
						}
					}
					if(isActiveMaxSize()) {
						// TODO MultiPathJsonMaxSizeFilter
						return new MultiPathMaxSizeMaxStringLengthJsonFilter(maxStringLength, maxSize, maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
					}
					return new MultiPathJsonFilter(maxPathMatches, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
			}
		} else if(maxPathMatches != -1) {
			throw new IllegalArgumentException("If no prune or anonymize paths exists, max path matches should not be set.");
		}
		
		if(isActiveMaxStringLength()) {
			if(isActiveMaxSize()) {
				if(removeWhitespace) {
					return new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(maxStringLength, maxSize, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				} else {
					return new MaxStringLengthMaxSizeJsonFilter(maxStringLength, maxSize, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
			} else {
				if(removeWhitespace) {
					return new MaxStringLengthRemoveWhitespaceJsonFilter(maxStringLength, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				} else {
					return new MaxStringLengthJsonFilter(maxStringLength, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
			}
		}
		
		if(removeWhitespace) {
			return new RemoveWhitespaceJsonFilter();
		}

		return new DefaultJsonFilter();
	}


}
