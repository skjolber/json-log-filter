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
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath());
			if(jsonFilter != null) {
				sink.write(precorrelation, new JsonFilterHttpRequest(request, jsonFilter, compactRequests, validateRequests, factory));
			} else if(validateRequests || compactRequests) {
				sink.write(precorrelation, new JsonFilterHttpRequest(request, jsonFilter, compactRequests, validateRequests, factory));
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
			JsonFilter jsonFilter = filter.getResponseFilter(request.getPath());
			if(jsonFilter != null) {
				sink.write(correlation, request, new JsonFilterHttpResponse(response, jsonFilter, compactResponses, validateResponses, factory));
			} else if(validateRequests || compactRequests) {
				sink.write(correlation, request, new JsonHttpResponse(response, compactResponses, validateResponses, factory));
			} else {
				sink.write(correlation, request, response);
			}
		} else {
			sink.write(correlation, request, response);
		}
	}

}
