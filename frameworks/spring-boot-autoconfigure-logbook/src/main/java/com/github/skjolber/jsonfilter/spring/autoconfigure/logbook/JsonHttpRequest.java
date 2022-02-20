package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;
import java.util.Optional;

import org.zalando.logbook.HttpRequest;

public class JsonHttpRequest extends AbstractFilterHttpMessage<HttpRequest> implements HttpRequest {

	public JsonHttpRequest(HttpRequest request, JsonProcessor jsonProcessor) {
		super(request, jsonProcessor);
	}

	public String getRemote() {
		return message.getRemote();
	}

	public String getMethod() {
		return message.getMethod();
	}

	public String getScheme() {
		return message.getScheme();
	}

	public String getHost() {
		return message.getHost();
	}

	public Optional<Integer> getPort() {
		return message.getPort();
	}

	public String getPath() {
		return message.getPath();
	}

	public String getQuery() {
		return message.getQuery();
	}

	public HttpRequest withBody() throws IOException {
		return message.withBody();
	}

	public HttpRequest withoutBody() {
		return message.withoutBody();
	}

}