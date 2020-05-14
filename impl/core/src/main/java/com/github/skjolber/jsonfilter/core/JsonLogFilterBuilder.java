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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonLogFilterBuilder {

	public static JsonLogFilterBuilder createInstance() {
		return new JsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();
	
	public JsonFilter build() {
		JsonFilterFactory factory = new JsonFilterFactory();
		
		factory.setMaxStringLength(maxStringLength);
		factory.setMaxPathMatches(maxPathMatches);
		
		if(!anonymizeFilters.isEmpty()) {
			factory.setAnonymizeFilters(anonymizeFilters);
		}
		if(!pruneFilters.isEmpty()) {
			factory.setPruneFilters(pruneFilters);
		}

		return factory.newJsonFilter();
	}
	
	public JsonLogFilterBuilder withMaxStringLength(int length) {
		this.maxStringLength = length;
		
		return this;
	}	
		
	public JsonLogFilterBuilder withMaxPathMatches(int length) {
		this.maxPathMatches = length;
		
		return this;
	}	
	
	public JsonLogFilterBuilder withPrune(String ... filters) {
		Collections.addAll(pruneFilters, filters);
		
		return this;
	}
	
	public JsonLogFilterBuilder withAnonymize(String ... filters) {
		Collections.addAll(anonymizeFilters, filters);
		return this;
	}
	
}
