package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import org.zalando.logbook.HttpResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterHttpResponse extends AbstractJsonFilterHttpMessage<HttpResponse> implements HttpResponse {

	public JsonFilterHttpResponse(HttpResponse response, JsonFilter filter, boolean compact, boolean validate, JsonFactory factory) {
		super(response, filter, compact, validate, factory);
	}
	
	public int getStatus() {
		return message.getStatus();
	}

	public HttpResponse withBody() throws IOException {
		return message.withBody();
	}

	public HttpResponse withoutBody() {
		return message.withoutBody();
	}

	public String getContentType() {
		return message.getContentType();
	}

}
