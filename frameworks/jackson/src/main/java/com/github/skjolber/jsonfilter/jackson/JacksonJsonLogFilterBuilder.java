package com.github.skjolber.jsonfilter.jackson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JacksonJsonLogFilterBuilder {

	public static JacksonJsonLogFilterBuilder createInstance() {
		return new JacksonJsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();
	
	protected String pruneMessage;
	protected String anonymizeMessage;
	protected String truncateMessage;
	
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
		
	public JacksonJsonLogFilterBuilder withPruneMessage(String message) {
		this.pruneMessage = message;
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withAnonymizeMessage(String message) {
		this.anonymizeMessage = message;
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withTruncateMessage(String message) {
		this.truncateMessage = message;
		
		return this;
	}
	
}
