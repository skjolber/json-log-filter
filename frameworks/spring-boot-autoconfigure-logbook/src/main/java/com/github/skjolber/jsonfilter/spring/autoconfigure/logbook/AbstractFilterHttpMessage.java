package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.Origin;

public abstract class AbstractFilterHttpMessage<B extends HttpMessage> {

	protected final B message;
	protected final JsonProcessor jsonProcessor;

	public AbstractFilterHttpMessage(B request, JsonProcessor jsonProcessor) {
		this.jsonProcessor = jsonProcessor;
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
		return jsonProcessor.processBody(message.getBody());
	}

	public String getBodyAsString() throws IOException {
		return jsonProcessor.processBody(message.getBodyAsString());
	}

}
