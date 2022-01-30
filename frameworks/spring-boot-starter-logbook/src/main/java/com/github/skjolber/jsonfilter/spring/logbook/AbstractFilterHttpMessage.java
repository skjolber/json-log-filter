package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.Origin;

import com.fasterxml.jackson.core.JsonFactory;

public abstract class AbstractFilterHttpMessage<B extends HttpMessage> extends JsonProcessor {

	protected final B message;

	public AbstractFilterHttpMessage(B request, boolean compact, boolean validate, JsonFactory factory) {
		super(compact, validate, factory);
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
			if(validate) {
				// will also compact
				try {
					return validate(body);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compact) {
				return compact(body);
			} else {
				return body;
			}
			return handleInvalidBodyAsBytes(body);
		}
		return body;
	}

	public String getBodyAsString() throws IOException {
		String body = message.getBodyAsString();
		if(body != null) {
			if(validate) {
				// will also compact
				try {
					return validate(body);
				} catch (Exception e) {
					// fall through to escape as string
				}
			} else if(compact) {
				return compact(body);
			} else {
				return body;
			}
			return handleInvalidBodyAsString(body);
		}
		return body;
	}

}
