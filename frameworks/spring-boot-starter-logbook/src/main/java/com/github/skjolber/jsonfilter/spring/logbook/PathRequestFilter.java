package com.github.skjolber.jsonfilter.spring.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RequestFilter;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;

/**
 * Convenience class for filtering requests.
 * 
 */
public class PathRequestFilter implements RequestFilter {

	protected final RequestResponseJsonFilter filter;
	protected final boolean compact;
	protected final boolean validate;
	protected final JsonFactory jsonFactory;

	public PathRequestFilter(RequestResponseJsonFilter filter, boolean validate, boolean compact, JsonFactory jsonFactory) {
		this.filter = filter;
		this.validate = validate;
		this.compact = compact;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public HttpRequest filter(HttpRequest request) {
		if(PathFilterSink.isJson(request.getContentType())) {
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath());
			if(jsonFilter != null) {
				return new JsonFilterHttpRequest(request, jsonFilter, compact, validate, jsonFactory);
			} else if(validate || compact) {
				return new JsonHttpRequest(request, compact, validate, jsonFactory);
			}
		}
		
		return request;
	}
	
}
