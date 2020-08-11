package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import com.github.skjolber.jsonfilter.JsonFilter;

public class JsonFilterHttpResponse implements HttpResponse {

	private final HttpResponse response;
	private final JsonFilter filter;

	public JsonFilterHttpResponse(HttpResponse response, JsonFilter filter) {
		super();
		this.response = response;
		this.filter = filter;
	}

	public int getStatus() {
		return response.getStatus();
	}

	public HttpResponse withBody() throws IOException {
		return response.withBody();
	}

	public String getProtocolVersion() {
		return response.getProtocolVersion();
	}

	public Origin getOrigin() {
		return response.getOrigin();
	}

	public HttpHeaders getHeaders() {
		return response.getHeaders();
	}

	public HttpResponse withoutBody() {
		return response.withoutBody();
	}

	public String getContentType() {
		return response.getContentType();
	}

	public String getReasonPhrase() {
		return response.getReasonPhrase();
	}

	public Charset getCharset() {
		return response.getCharset();
	}

	public byte[] getBody() throws IOException {
		byte[] body = response.getBody();
		if(body != null) {
			byte[] filtered = filter.process(body);
			if(filtered != null) {
				return filtered;
			}
		}
		return body;
	}

	public String getBodyAsString() throws IOException {
		String bodyAsString = response.getBodyAsString();
		if(bodyAsString != null) {
			String filtered = filter.process(bodyAsString);
			if(filtered != null) {
				return filtered;
			}
		}
		return bodyAsString;
	}

}
