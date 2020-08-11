package com.github.skjolber.jsonfilter.spring.logbook;

import org.zalando.logbook.BodyFilter;

import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * Convenience class for filtering bodies.
 * 
 */

public class JsonBodyFilter implements BodyFilter {

	protected final JsonFilter jsonFilter;
	
	public JsonBodyFilter(JsonFilter jsonFilter) {
		this.jsonFilter = jsonFilter;
	}

	@Override
	public String filter(String contentType, String body) {
		if(PathFilterSink.isJson(contentType)) {
			String filtered = jsonFilter.process(body);
			if(filtered != null) {
				return filtered;
			}
		}
		return body;
	}

}
