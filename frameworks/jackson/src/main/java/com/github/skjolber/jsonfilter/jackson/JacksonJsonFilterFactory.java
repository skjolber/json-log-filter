/***************************************************************************
 * Copyright 2016 Thomas Rorvik Skjolberg
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
package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

/**
 * Jackson {@linkplain JsonFilter} factory.
 * <br><br>
 * Property maxFilterMatches is ignored.
 * 
 */

public class JacksonJsonFilterFactory extends AbstractJsonFilterFactory {
		
	/**
	 * Spawn a factory instance. Equivalent to using the default constructor.
	 * 
	 * @return newly created {@linkplain JacksonJsonFilterFactory}.
	 */
	
	public static JacksonJsonFilterFactory newInstance() {
		return new JacksonJsonFilterFactory();
	}
	
	/**
	 * Spawn a filter. 
	 * 
	 * @return new, or previously created, thread-safe pretty printer
	 */
	
	public JsonFilter newJsonFilter() {
		// check for any prune/anon filter
		
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
		
		if(isActivePathFilters()) {
			// check for single prune/anon filter
			if(isSinglePruneFilter()) {
				if(!AbstractPathJsonFilter.hasAnyPrefix(pruneFilters[0])) {
					return new JacksonSinglePathMaxStringLengthJsonFilter(maxStringLength, pruneFilters[0], FilterType.PRUNE, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
			} else if(isSingleAnonymizeFilter()) {
				if(!AbstractPathJsonFilter.hasAnyPrefix(anonymizeFilters[0])) {				
					return new JacksonSinglePathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters[0], FilterType.ANON, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
				}
			}
			if(!isFullPrefix(anonymizeFilters) && !isFullPrefix(pruneFilters)) {
				return new JacksonMultiAnyPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
			}
		
			return new JacksonMultiPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		}
		if(isActiveMaxStringLength()) {
			return new JacksonMaxStringLengthJsonFilter(maxStringLength, pruneJsonValue, anonymizeJsonValue, truncateStringValue);
		}

		return new DefaultJsonFilter();
	}

	protected boolean isAnyPrefix(String[] filters) {
		return filters != null && AbstractPathJsonFilter.hasAnyPrefix(filters);
	}

	protected boolean isFullPrefix(String[] filters) {
		return filters != null && !AbstractPathJsonFilter.hasAnyPrefix(filters);
	}
	

}
