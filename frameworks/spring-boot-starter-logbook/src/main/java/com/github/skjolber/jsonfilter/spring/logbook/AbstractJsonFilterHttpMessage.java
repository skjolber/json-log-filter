package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.Origin;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.JsonFilter;

public abstract class AbstractJsonFilterHttpMessage<B extends HttpMessage> extends JsonFilterProcessor {

	protected final B message;

	public AbstractJsonFilterHttpMessage(B request, JsonFilter filter, boolean compact, boolean validate, JsonFactory factory) {
		super(filter, compact, validate, factory);
		this.message = request;
	}

	public String getProtocolVersion() {
		return message.getProtocolVersion();
	}

	public Origin getOrigin() {
		return message.getOrigin();
	}

	public HttpHeaders getHeaders() {
		return message.getHeaders();
	}

	public String getContentType() {
		return message.getContentType();
	}

	public Charset getCharset() {
		return message.getCharset();
	}

	public byte[] getBody() throws IOException {
		byte[] body = message.getBody();
		if(body != null) {
			return handleBodyAsBytes(body);
		}
		return null;
	}

	public String getBodyAsString() throws IOException {
		String bodyAsString = message.getBodyAsString();
		if(bodyAsString != null) {
			return handleBodyAsString(bodyAsString);
		}
		return null;
	}

}
