package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterHttpRequest implements HttpRequest {

	private final HttpRequest request;
	private final JsonFilter filter;

	public JsonFilterHttpRequest(HttpRequest request, JsonFilter filter) {
		super();
		this.request = request;

		this.filter = filter;
	}

	public String getRemote() {
		return request.getRemote();
	}

	public String getProtocolVersion() {
		return request.getProtocolVersion();
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getRequestUri() {
		return request.getRequestUri();
	}

	public Origin getOrigin() {
		return request.getOrigin();
	}

	public HttpHeaders getHeaders() {
		return request.getHeaders();
	}

	public String getContentType() {
		return request.getContentType();
	}

	public Charset getCharset() {
		return request.getCharset();
	}

	public byte[] getBody() throws IOException {
		byte[] body = request.getBody();
		if(body != null) {
			byte[] filtered = filter.process(body);
			if(filtered != null) {
				return filtered;
			}
		}
		return body;
	}

	public String getBodyAsString() throws IOException {
		String bodyAsString = request.getBodyAsString();
		if(bodyAsString != null) {
			String filtered = filter.process(bodyAsString);
			if(filtered != null) {
				return filtered;
			}
		}
		return bodyAsString;
	}

	public String getScheme() {
		return request.getScheme();
	}

	public String getHost() {
		return request.getHost();
	}

	public Optional<Integer> getPort() {
		return request.getPort();
	}

	public String getPath() {
		return request.getPath();
	}

	public String getQuery() {
		return request.getQuery();
	}

	public HttpRequest withBody() throws IOException {
		return request.withBody();
	}

	public HttpRequest withoutBody() {
		return request.withoutBody();
	}



}