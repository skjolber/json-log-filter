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
		
		factory.setAnonymizeJsonValue(anonymizeJsonValue);
		factory.setPruneJsonValue(pruneJsonValue);
		factory.setTruncateJsonStringValue(truncateStringValue);

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
		
	/**
	 * Set a textual value as use for replacement for pruned value(s)
	 * 
	 * @param value the (unescaped) message
	 * 
	 * @return this instance
	 */

	public JacksonJsonLogFilterBuilder withPruneStringValue(String value) {
		StringBuilder stringBuilder = new StringBuilder(value.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(value, stringBuilder);
		stringBuilder.append('"');
		return withPruneRawJsonValue(stringBuilder.toString());
	}

	/**
	 * Set a textual value as use for replacement for anonymized value(s)
	 * 
	 * @param value the (unescaped) message
	 * 
	 * @return this instance
	 */

	public JacksonJsonLogFilterBuilder withAnonymizeStringValue(String value) {
		StringBuilder stringBuilder = new StringBuilder(value.length() * 2);
		stringBuilder.append('"');
		AbstractJsonFilter.quoteAsString(value, stringBuilder);
		stringBuilder.append('"');
		return withAnonymizeRawJsonValue(stringBuilder.toString());
	}
	
	/**
	 * Set the truncate textual value.
	 * 
	 * @param value the (unescaped) message
	 * 
	 * @return this instance
	 */
	
	public JacksonJsonLogFilterBuilder withTruncateStringValue(String value) {
		StringBuilder stringBuilder = new StringBuilder(value.length() * 2);
		AbstractJsonFilter.quoteAsString(value, stringBuilder);
		return withTruncateRawJsonStringValue(stringBuilder.toString());
	}
	
	public JacksonJsonLogFilterBuilder withTruncateRawJsonStringValue(String escaped) {
		this.truncateStringValue = escaped;
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withPruneRawJsonValue(String raw) {
		this.pruneJsonValue = raw;
		
		return this;
	}

	public JacksonJsonLogFilterBuilder withAnonymizeRawJsonValue(String raw) {
		this.anonymizeJsonValue = raw;
		
		return this;
	}	
	
}
