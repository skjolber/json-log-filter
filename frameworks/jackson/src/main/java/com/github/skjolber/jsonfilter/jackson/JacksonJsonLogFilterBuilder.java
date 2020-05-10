package com.github.skjolber.jsonfilter.jackson;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JacksonJsonLogFilterBuilder {

	public static JacksonJsonLogFilterBuilder createInstance() {
		return new JacksonJsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();
	
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
		for(String filter : filters) {
			pruneFilters.add(filter);
		}
		
		return this;
	}
	
	public JacksonJsonLogFilterBuilder withAnonymize(String ... filters) {
		for(String filter : filters) {
			anonymizeFilters.add(filter);
		}
		
		return this;
	}
		
	
	
}
