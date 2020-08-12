package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.spring.RequestResponseJsonFilter;

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
	
	public PathFilterSink(Sink sink, RequestResponseJsonFilter filter) {
		this.sink = sink;
		this.filter = filter;
	}

	@Override
	public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
		if(isJson(request.getContentType())) {
			JsonFilter jsonFilter = filter.getRequestFilter(request.getPath());
			if(jsonFilter != null) {
				request = new JsonFilterHttpRequest(request, jsonFilter);
			}
		}
		sink.write(precorrelation, request);
	}

	@Override
	public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
		if(isJson(response.getContentType())) {
			JsonFilter jsonFilter = filter.getResponseFilter(request.getPath());
			if(jsonFilter != null) {
				response = new JsonFilterHttpResponse(response, jsonFilter);
			}
		}
		sink.write(correlation, request, response);
	}

}
