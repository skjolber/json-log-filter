package com.github.skjolber.jsonfilter.jackson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonJsonLogFilterBuilder {

	public static JacksonJsonLogFilterBuilder createInstance() {
		return new JacksonJsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();

	/** Raw JSON */
	protected String pruneJsonValue;
	/** Raw JSON */
	protected String anonymizeJsonValue;
	
	/** Raw (escaped) JSON string */
	protected String truncateStringValue;
	
	public JsonFilter build() {
		JacksonJsonFilterFactory factory = new JacksonJsonFilterFactory();
		
		factory.setMaxStringLength(maxStringLength);
		if(!anonymizeFilters.isEmpty()) {
			factory.setAnonymizeFilters(anonymizeFilters);
		}
		if(!pruneFilters.isEmpty()) {
			factory.setPruneFilters(pruneFilters);
		}

		return factory.newJsonFilter();
	}
	
	public JacksonJsonLogFilterBuilder withMaxStringLength(int length) {
		this.maxStringLength = length;
		
		return this;
	}	
	
	public JacksonJsonLogFilterBuilder withPrune(String ... filters) {
		Collections.addAll(pruneFilters, filters);
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withAnonymize(String ... filters) {
		Collections.addAll(anonymizeFilters, filters);
		return this;
	}
		
	public JacksonJsonLogFilterBuilder withPruneStringValue(String pruneMessage) {
		StringBuilder stringBuilder = new StringBuilder(pruneMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(pruneMessage, stringBuilder);
		stringBuilder.append('"');
		return withPruneJsonValue(stringBuilder.toString());
	}

	public JacksonJsonLogFilterBuilder withAnonymizeStringValue(String anonymizeMessage) {
		StringBuilder stringBuilder = new StringBuilder(anonymizeMessage.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(anonymizeMessage, stringBuilder);
		stringBuilder.append('"');
		return withAnonymizeJsonValue(stringBuilder.toString());
	}

	public JacksonJsonLogFilterBuilder withTruncateStringValue(String truncateMessage) {
		StringBuilder stringBuilder = new StringBuilder(truncateMessage.length() * 2);
		AbstractJsonFilter.quoteAsString(truncateMessage, stringBuilder);
		return withTruncateJsonStringValue(stringBuilder.toString());
	}
	
	public JacksonJsonLogFilterBuilder withTruncateJsonStringValue(String escaped) {
		this.truncateStringValue = escaped;
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withPruneJsonValue(String raw) {
		this.pruneJsonValue = raw;
		
		return this;
	}

	public JacksonJsonLogFilterBuilder withAnonymizeJsonValue(String raw) {
		this.anonymizeJsonValue = raw;
		
		return this;
	}	
	
}
