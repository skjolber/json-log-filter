package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;

import org.zalando.logbook.Correlation;
import org.zalando.logbook.ForwardingHttpRequest;
import org.zalando.logbook.ForwardingHttpResponse;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

public class PathFilterSink implements Sink {

	public static boolean isJson(String contentType) {
		if(contentType == null) {
			return false;
		}
		// implementation note: manually coded for improved performance
		if(contentType.startsWith("application/")) {
			int index = contentType.indexOf(';', 12);
			if(index != -1) {
				if(index > 16) {
					// application/some+json;charset=utf-8
					return contentType.regionMatches(index - 5, "+json", 0, 5);
				}
				
				// application/json;charset=utf-8
				return contentType.regionMatches(index - 4, "json", 0, 4);
			} else {
				// application/json
				if(contentType.length() == 16) {
					return contentType.endsWith("json");
				}
				// application/some+json
				return contentType.endsWith("+json");
			}
		}
		return false;
	}
	
	protected final Sink sink;
	protected final RequestResponseJsonFilter filter;
	
	protected final boolean validateRequests;
	protected final boolean validateResponses;
	
	protected final WhitespaceStrategy requestWhitespaceStrategy;
	protected final WhitespaceStrategy responseWhitespaceStrategy;
	
	protected final JsonFactory factory;
	
	public PathFilterSink(Sink sink, RequestResponseJsonFilter filter, boolean validateRequests, boolean validateResponses, WhitespaceStrategy requestWhitespaceStrategy, WhitespaceStrategy responseWhitespaceStrategy, JsonFactory factory) {
		this.sink = sink;
		this.filter = filter;
		this.validateRequests = validateRequests;
		this.validateResponses = validateResponses;
		this.requestWhitespaceStrategy = requestWhitespaceStrategy;
		this.responseWhitespaceStrategy = responseWhitespaceStrategy;
		this.factory = factory;
	}

	@Override
	public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
		
		if(isJson(request.getContentType())) {
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
						sink.write(precorrelation, new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, requestWhitespaceStrategy)));
					} else if(requestWhitespaceStrategy != WhitespaceStrategy.NEVER) {
						sink.write(precorrelation, new JsonHttpRequest(request, new CompactingJsonProcessor()));
					} else {
						sink.write(precorrelation, request);
					}
					return;
				} else {
					// might still be valid JSON
				}
			}
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath(), validateRequests);
			if(jsonFilter != null) {
				if(validateRequests) {
					sink.write(precorrelation, new JsonHttpRequest(request, new ValidatingJsonFilterProcessor(jsonFilter, factory, requestWhitespaceStrategy)));
				} else {
					sink.write(precorrelation, new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, requestWhitespaceStrategy)));
				}
			} else if(validateRequests) {
				sink.write(precorrelation, new JsonHttpRequest(request, new ValidatingJsonProcessor(factory, requestWhitespaceStrategy)));
			} else if(requestWhitespaceStrategy != WhitespaceStrategy.NEVER) {
				sink.write(precorrelation, new JsonHttpRequest(request, new CompactingJsonProcessor()));
			} else {
				sink.write(precorrelation, request);
			}
		} else {
			sink.write(precorrelation, request);
		}
	}

	@Override
	public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
		if(isJson(response.getContentType())) {
			
			PreprocessedHttpMessage preprocessedHttpMessage = null;
			
			HttpResponse delegate = response; 
			do {
				if(delegate instanceof PreprocessedHttpResponse) {
					preprocessedHttpMessage = (PreprocessedHttpResponse) delegate;
					break;
				} else if(delegate instanceof ForwardingHttpResponse) {
					ForwardingHttpResponse forwarding = (ForwardingHttpResponse) delegate;
					
					delegate = forwarding.delegate();
				} else {
					break;
				}
			} while(true);
			
			if(preprocessedHttpMessage != null) {

				boolean databinding = preprocessedHttpMessage.isDatabindingPerformed() && preprocessedHttpMessage.wasDatabindingSuccessful();

				if(databinding) {
					// so no JSON validation is necessary
					JsonFilter jsonFilter = filter.getResponseFilter(request.getPath(), false);
					if(jsonFilter != null) {
						sink.write(correlation, request, new JsonHttpResponse(response, new JsonFilterProcessor(jsonFilter, responseWhitespaceStrategy)));
					} else if(requestWhitespaceStrategy != WhitespaceStrategy.NEVER) {
						sink.write(correlation, request, new JsonHttpResponse(response, new CompactingJsonProcessor()));
					} else {
						sink.write(correlation, request, response);
					}
					return;
				} else {
					// might still be valid JSON
				}
			}
			JsonFilter jsonFilter = filter.getResponseFilter(request.getPath(), validateResponses);
			if(jsonFilter != null) {
				if(validateResponses) {
					sink.write(correlation, request, new JsonHttpResponse(response, new ValidatingJsonFilterProcessor(jsonFilter, factory, responseWhitespaceStrategy)));
				} else {
					sink.write(correlation, request, new JsonHttpResponse(response, new JsonFilterProcessor(jsonFilter, responseWhitespaceStrategy)));
				}
			} else if(validateResponses) {
				sink.write(correlation, request, new JsonHttpResponse(response, new ValidatingJsonProcessor(factory, responseWhitespaceStrategy)));
			} else if(requestWhitespaceStrategy != WhitespaceStrategy.NEVER) {
				sink.write(correlation, request, new JsonHttpResponse(response, new CompactingJsonProcessor()));
			} else {
				sink.write(correlation, request, response);
			}
		} else {
			sink.write(correlation, request, response);
		}
	}

}
