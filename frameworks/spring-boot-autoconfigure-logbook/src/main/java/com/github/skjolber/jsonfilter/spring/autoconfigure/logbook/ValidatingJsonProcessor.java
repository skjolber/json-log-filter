package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.path.properties.WhitespaceStrategy;

public class ValidatingJsonProcessor extends JsonProcessor {

	protected final JsonFactory factory;
	protected final WhitespaceStrategy whitespaceStrategy;

	public ValidatingJsonProcessor(JsonFactory factory, WhitespaceStrategy whitespaceStrategy) {
		this.factory = factory;
		this.whitespaceStrategy = whitespaceStrategy;
	}

	protected String validate(final String json) throws IOException {
		try (
				final CharArrayWriter output = new CharArrayWriter(json.length());
				final JsonParser parser = factory.createParser(json);
				final JsonGenerator generator = factory.createGenerator(output)) {

			while (parser.nextToken() != null) {
				generator.copyCurrentEvent(parser);
			}

			generator.flush();

			return output.toString();
		}
	}

	protected byte[] validate(final byte[] json) throws IOException {
		try (
				final ByteArrayOutputStream output = new ByteArrayOutputStream(json.length);
				final JsonParser parser = factory.createParser(json);
				final JsonGenerator generator = factory.createGenerator(output)) {

			while (parser.nextToken() != null) {
				generator.copyCurrentEvent(parser);
			}

			generator.flush();

			return output.toByteArray();
		}
	}

	@Override
	public byte[] processBody(byte[] body) throws IOException {
		if(body != null) {
			// will also compact
			try {
				return validate(body);
			} catch (Exception e) {
				// fall through to escape as string
			}
			return handleInvalidBodyAsBytes(body, whitespaceStrategy);
		}
		return body;
	}
	
	@Override
	public String processBody(String body) throws IOException {
		if(body != null) {
			// will also compact
			try {
				return validate(body);
			} catch (Exception e) {
				// fall through to escape as string
			}
			return handleInvalidBodyAsString(body, whitespaceStrategy);
		}
		return body;
	}

}
