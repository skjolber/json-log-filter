package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;

import org.zalando.logbook.HttpResponse;

import com.fasterxml.jackson.core.JsonFactory;

public class JsonHttpResponse extends AbstractFilterHttpMessage<HttpResponse> implements HttpResponse {

	public JsonHttpResponse(HttpResponse response, boolean compact, boolean validate, JsonFactory factory) {
		super(response, compact, validate, factory);
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
