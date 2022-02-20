package com.github.skjolber.jsonfilter.spring.autoconfigure.logbook;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class ValidatingJsonProcessor extends JsonProcessor {

	protected final JsonFactory factory;
	protected final boolean compacting;

	public ValidatingJsonProcessor(JsonFactory factory, boolean compacting) {
		this.factory = factory;
		this.compacting = compacting;
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
			return handleInvalidBodyAsBytes(body, compacting);
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
			return handleInvalidBodyAsString(body, compacting);
		}
		return body;
	}

}
