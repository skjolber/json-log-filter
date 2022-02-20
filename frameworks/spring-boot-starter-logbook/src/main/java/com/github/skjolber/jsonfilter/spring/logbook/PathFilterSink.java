package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.path.RequestResponseJsonFilter;

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
	
	protected final boolean compactRequests;
	protected final boolean compactResponses;
	
	protected JsonFactory factory;
	
	public PathFilterSink(Sink sink, RequestResponseJsonFilter filter, boolean validateRequests, boolean validateResponses, boolean compactRequests, boolean compactResponses, JsonFactory factory) {
		this.sink = sink;
		this.filter = filter;
		this.validateRequests = validateRequests;
		this.validateResponses = validateResponses;
		this.compactRequests = compactRequests;
		this.compactResponses = compactResponses;
		this.factory = factory;
	}

	@Override
	public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
		if(isJson(request.getContentType())) {
			if(request instanceof PreprocessedHttpRequest) {
				PreprocessedHttpRequest preprocessedHttpMessage = (PreprocessedHttpRequest)request;
				
				boolean databinding = preprocessedHttpMessage.isDatabindingPerformed() && preprocessedHttpMessage.wasDatabindingSuccessful();

				if(databinding) {
					// so no JSON validation is necessary
					JsonFilter jsonFilter = filter.getRequestFilter(request.getPath(), false);
					
					if(jsonFilter != null) {
						sink.write(precorrelation, new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, compactRequests)));
					} else if(compactRequests) {
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
					sink.write(precorrelation, new JsonHttpRequest(request, new ValidatingJsonFilterProcessor(jsonFilter, factory, compactRequests)));
				} else {
					sink.write(precorrelation, new JsonHttpRequest(request, new JsonFilterProcessor(jsonFilter, compactRequests)));
				}
			} else if(validateRequests) {
				sink.write(precorrelation, new JsonHttpRequest(request, new ValidatingJsonProcessor(factory, compactRequests)));
			} else if(compactRequests) {
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
			if(response instanceof PreprocessedHttpResponse) {
				PreprocessedHttpMessage preprocessedHttpMessage = (PreprocessedHttpMessage)response;

				boolean databinding = preprocessedHttpMessage.isDatabindingPerformed() && preprocessedHttpMessage.wasDatabindingSuccessful();

				if(databinding) {
					// so no JSON validation is necessary
					JsonFilter jsonFilter = filter.getResponseFilter(request.getPath(), false);
					
					if(jsonFilter != null) {
						sink.write(correlation, request, new JsonHttpResponse(response, new JsonFilterProcessor(jsonFilter, compactResponses)));
					} else if(compactRequests) {
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
				if(validateRequests) {
					sink.write(correlation, request, new JsonHttpResponse(response, new ValidatingJsonFilterProcessor(jsonFilter, factory, compactRequests)));
				} else {
					sink.write(correlation, request, new JsonHttpResponse(response, new JsonFilterProcessor(jsonFilter, compactRequests)));
				}
			} else if(validateRequests) {
				sink.write(correlation, request, new JsonHttpResponse(response, new ValidatingJsonProcessor(factory, compactRequests)));
			} else if(compactRequests) {
				sink.write(correlation, request, new JsonHttpResponse(response, new CompactingJsonProcessor()));
			} else {
				sink.write(correlation, request, response);
			}
		} else {
			sink.write(correlation, request, response);
		}
	}

}
