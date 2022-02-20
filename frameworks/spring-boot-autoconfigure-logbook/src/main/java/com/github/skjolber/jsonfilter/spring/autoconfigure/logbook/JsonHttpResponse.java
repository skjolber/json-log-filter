package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;

import org.zalando.logbook.HttpResponse;

public class JsonHttpResponse extends AbstractFilterHttpMessage<HttpResponse> implements HttpResponse {

	public JsonHttpResponse(HttpResponse response, JsonProcessor jsonProcessor) {
		super(response, jsonProcessor);
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
