package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import org.zalando.logbook.ForwardingHttpRequest;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RequestFilter;
import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

public class PathResponseFilter implements RequestFilter {

	protected final RequestResponseJsonFilter filter;
	
	protected final boolean validate;
	protected final WhitespaceStrategy whitespaceStrategy;
	protected final JsonFactory factory;
	
	public PathResponseFilter(RequestResponseJsonFilter filter, boolean validate, WhitespaceStrategy whitespaceStrategy, JsonFactory factory) {
		this.filter = filter;
		this.validate = validate;
		this.whitespaceStrategy = whitespaceStrategy;
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
						return new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, whitespaceStrategy));
					} else if(whitespaceStrategy != WhitespaceStrategy.NEVER) {
						return new JsonHttpRequest(request, new CompactingJsonProcessor());
					}
					return request;
				} else {
					// might still be valid JSON
				}
			}
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath(), validate);
			if(jsonFilter != null) {
				if(validate) {
					return new JsonHttpRequest(request, new ValidatingJsonFilterProcessor(jsonFilter, factory, whitespaceStrategy));
				} else {
					return new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, whitespaceStrategy));
				}
			} else if(validate) {
				return new JsonHttpRequest(request, new ValidatingJsonProcessor(factory, whitespaceStrategy));
			} else if(whitespaceStrategy != WhitespaceStrategy.NEVER) {
				return new JsonHttpRequest(request, new CompactingJsonProcessor());
			} else {
				return request;
			}
		} else {
			return request;
		}
	}

}
