package com.github.skjolber.jsonfilter.core;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonLogFilterBuilder {

	public static JsonLogFilterBuilder createInstance() {
		return new JsonLogFilterBuilder();
	}
	
	protected int maxStringLength = -1;
	
	protected List<String> anonymizeFilters = new ArrayList<>();
	protected List<String> pruneFilters = new ArrayList<>();
	
	public JsonFilter build() {
		JsonFilterFactory factory = new JsonFilterFactory();
		
		factory.setMaxStringLength(maxStringLength);
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
	
	public JsonLogFilterBuilder withPrune(String ... filters) {
		for(String filter : filters) {
			pruneFilters.add(filter);
		}
		
		return this;
	}
	
	public JsonLogFilterBuilder withAnonymize(String ... filters) {
		for(String filter : filters) {
			anonymizeFilters.add(filter);
		}
		
		return this;
	}
		
	
	
}
