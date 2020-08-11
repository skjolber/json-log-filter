package com.github.skjolber.jsonfilter.spring.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RequestFilter;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.spring.RequestResponseJsonFilter;

/**
 * Convenience class for filtering requests.
 * 
 */
public class PathRequestFilter implements RequestFilter {

	protected final RequestResponseJsonFilter filter;
	
	public PathRequestFilter(RequestResponseJsonFilter filter) {
		this.filter = filter;
	}

	@Override
	public HttpRequest filter(HttpRequest request) {
		if(PathFilterSink.isJson(request.getContentType())) {
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath());
			if(jsonFilter != null) {
				return new JsonFilterHttpRequest(request, jsonFilter);
			}
		}
		
		return request;
	}

}
