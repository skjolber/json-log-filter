package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.zalando.logbook.ForwardingHttpRequest;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RequestFilter;
import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;

public class PathRequestFilter implements RequestFilter {

	protected final RequestResponseJsonFilter filter;
	
	protected final boolean validateRequests;
	protected final boolean compactRequests;
	protected final JsonFactory factory;
	
	public PathRequestFilter(RequestResponseJsonFilter filter, boolean validateRequests, boolean compactRequests, JsonFactory factory) {
		this.filter = filter;
		this.validateRequests = validateRequests;
		this.compactRequests = compactRequests;
		this.factory = factory;
	}
	
	@Override
	public HttpRequest filter(HttpRequest request) {
		if(PathFilterSink.isJson(request.getContentType())) {
			PreprocessedHttpRequest preprocessedHttpMessage = null;
			
			HttpRequest delegate = request; 
			do {
				if(delegate instanceof PreprocessedHttpRequest) {
					preprocessedHttpMessage = (PreprocessedHttpRequest) delegate;
					break;
				} else if(delegate instanceof ForwardingHttpRequest) {
					ForwardingHttpRequest forwarding = (ForwardingHttpRequest) delegate;
					
					delegate = forwarding.delegate();
				} else {
					break;
				}
			} while(true);
			
			if(preprocessedHttpMessage != null) {
				boolean databinding = preprocessedHttpMessage.isDatabindingPerformed() && preprocessedHttpMessage.wasDatabindingSuccessful();

				if(databinding) {
					// so no further JSON validation is necessary
					JsonFilter jsonFilter = filter.getRequestFilter(request.getPath(), false);

					if(jsonFilter != null) {
						return new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, compactRequests));
					} else if(compactRequests) {
						return new JsonHttpRequest(request, new CompactingJsonProcessor());
					}
					return request;
				} else {
					// might still be valid JSON
				}
			}
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath(), validateRequests);
			if(jsonFilter != null) {
				if(validateRequests) {
					return new JsonHttpRequest(request, new ValidatingJsonFilterProcessor(jsonFilter, factory, compactRequests));
				} else {
					return new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, compactRequests));
				}
			} else if(validateRequests) {
				return new JsonHttpRequest(request, new ValidatingJsonProcessor(factory, compactRequests));
			} else if(compactRequests) {
				return new JsonHttpRequest(request, new CompactingJsonProcessor());
			} else {
				return request;
			}
		} else {
			return request;
		}
	}

}
