package com.github.skjolber.jsonfilter.spring.logbook;

import org.zalando.logbook.BodyFilter;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;

/**
 * Convenience class for filtering bodies.
 * 
 */

public class JsonBodyFilter extends JsonFilterProcessor implements BodyFilter {

	public JsonBodyFilter(JsonFilter filter, boolean compact, boolean validate) {
		this(filter, compact, validate, new JsonFactory());
	}

	public JsonBodyFilter(JsonFilter filter, boolean compact, boolean validate, JsonFactory factory) {
		super(filter, compact, validate, factory);
	}
	
	@Override
	public String filter(String contentType, String bodyAsString) {
		if(PathFilterSink.isJson(contentType)) {
			if(bodyAsString != null) {
				return handleBodyAsString(bodyAsString);
			}
			return null;
		}
		return bodyAsString;
	}
}
