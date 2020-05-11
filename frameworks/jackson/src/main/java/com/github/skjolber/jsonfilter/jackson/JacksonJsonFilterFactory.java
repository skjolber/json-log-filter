/***************************************************************************
 * Copyright 2016 Thomas Rorvik Skjolberg
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
package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilterFactory;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

/**
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
		if(isActivePathFilters()) {
			// check for single prune/anon filter
			if(isSinglePruneFilter()) {
				if(!pruneFilters[0].startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
					return new JacksonSinglePathMaxStringLengthJsonFilter(maxStringLength, pruneFilters[0], FilterType.PRUNE);
				}
			} else if(isSingleAnonymizeFilter() && !anonymizeFilters[0].startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
				return new JacksonSinglePathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters[0], FilterType.ANON);
			}
			if(!isFullPrefix(anonymizeFilters) && !isFullPrefix(pruneFilters)) {
				return new JacksonMultiAnyPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters);
			}
		
			return new JacksonMultiPathMaxStringLengthJsonFilter(maxStringLength, anonymizeFilters, pruneFilters);
		}
		if(isActiveMaxStringLength()) {
			return new JacksonMaxStringLengthJsonFilter(maxStringLength);
		}

		return new DefaultJsonFilter();
	}

	protected boolean isAnyPrefix(String[] filters) {
		return filters != null && hasAny(filters);
	}

	private boolean hasAny(String[] filters) {
		for(String string : filters) {
			if(string.startsWith(AbstractPathJsonFilter.ANY_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isFullPrefix(String[] filters) {
		return filters != null && !hasAny(filters);
	}
	

}
