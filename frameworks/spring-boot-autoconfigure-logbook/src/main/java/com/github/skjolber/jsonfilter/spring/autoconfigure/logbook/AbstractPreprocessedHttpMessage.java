package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.Origin;

public abstract class AbstractPreprocessedHttpMessage<B extends HttpMessage> implements PreprocessedHttpMessage {

	protected final B message;

	protected final boolean databindingPerformed;
	protected final boolean databindingSuccessful; 

	public AbstractPreprocessedHttpMessage(B message, boolean databindingPerformed, boolean databindingSuccessful) {
		this.message = message;
		this.databindingPerformed = databindingPerformed;
		this.databindingSuccessful = databindingSuccessful;
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
		return message.getBody();
	}

	public String getBodyAsString() throws IOException {
		return message.getBodyAsString();
	}

	@Override
	public boolean isDatabindingPerformed() {
		return databindingPerformed;
	}

	@Override
	public boolean wasDatabindingSuccessful() {
		return databindingSuccessful;
	}
}
