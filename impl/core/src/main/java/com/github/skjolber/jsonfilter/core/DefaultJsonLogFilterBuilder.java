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
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class DefaultJsonLogFilterBuilder {

	public static DefaultJsonLogFilterBuilder createInstance() {
		return new DefaultJsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	protected int maxPathMatches = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();
	
	/** Raw JSON */
	protected String pruneJsonValue;
	/** Raw JSON */
	protected String anonymizeJsonValue;
	
	/** Raw (escaped) JSON string */
	protected String truncateStringValue;
	
	public JsonFilter build() {
		DefaultJsonFilterFactory factory = new DefaultJsonFilterFactory();
		
		factory.setMaxStringLength(maxStringLength);
		factory.setMaxPathMatches(maxPathMatches);
		
		factory.setAnonymizeFilters(anonymizeFilters);
		factory.setPruneFilters(pruneFilters);

		factory.setPruneJsonValue(pruneJsonValue);
		factory.setAnonymizeJsonValue(anonymizeJsonValue);
		factory.setTruncateJsonStringValue(truncateStringValue);
		
		return factory.newJsonFilter();
	}
	
	public DefaultJsonLogFilterBuilder withMaxStringLength(int length) {
		this.maxStringLength = length;
		
		return this;
	}	
		
	public DefaultJsonLogFilterBuilder withMaxPathMatches(int length) {
		this.maxPathMatches = length;
		
		return this;
	}	
	
	public DefaultJsonLogFilterBuilder withPrune(String ... filters) {
		Collections.addAll(pruneFilters, filters);
		
		return this;
	}
	
	public DefaultJsonLogFilterBuilder withAnonymize(String ... filters) {
		Collections.addAll(anonymizeFilters, filters);
		return this;
	}

	public DefaultJsonLogFilterBuilder withPruneStringValue(String pruneMessage) {
		StringBuilder stringBuilder = new StringBuilder(pruneMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(pruneMessage, stringBuilder);
		stringBuilder.append('"');
		return withPruneJsonValue(stringBuilder.toString());
	}

	public DefaultJsonLogFilterBuilder withAnonymizeStringValue(String anonymizeMessage) {
		StringBuilder stringBuilder = new StringBuilder(anonymizeMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(anonymizeMessage, stringBuilder);
		stringBuilder.append('"');
		return withAnonymizeJsonValue(stringBuilder.toString());
	}

	public DefaultJsonLogFilterBuilder withTruncateStringValue(String truncateMessage) {
		StringBuilder stringBuilder = new StringBuilder(truncateMessage.length() * 2);
		AbstractJsonFilter.quoteAsString(truncateMessage, stringBuilder);
		return withTruncateJsonStringValue(stringBuilder.toString());
	}
	
	public DefaultJsonLogFilterBuilder withTruncateJsonStringValue(String escaped) {
		this.truncateStringValue = escaped;
		
		return this;
	}
	
	public DefaultJsonLogFilterBuilder withPruneJsonValue(String raw) {
		this.pruneJsonValue = raw;
		
		return this;
	}

	public DefaultJsonLogFilterBuilder withAnonymizeJsonValue(String raw) {
		this.anonymizeJsonValue = raw;
		
		return this;
	}	
	
}
