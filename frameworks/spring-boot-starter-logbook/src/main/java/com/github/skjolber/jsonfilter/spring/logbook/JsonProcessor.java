package com.github.skjolber.jsonfilter.spring.logbook;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.JsonStringEncoder;

public class JsonProcessor {

	protected final boolean compact;
	protected final boolean validate;
	protected final JsonFactory factory;

	public JsonProcessor(boolean compact, boolean validate, JsonFactory factory) {
		super();
		this.compact = compact;
		this.validate = validate;
		this.factory = factory;
	}

	protected String handleInvalidBodyAsString(String bodyAsString) {
		if(compact) {
			bodyAsString = compact(bodyAsString);
		}

		// escape as string
		return escapeAsJsonTextValue(bodyAsString);
	}
	
	protected String escapeAsJsonTextValue(String filtered) {
		StringBuilder output = new StringBuilder();
		output.append('"');
		JsonStringEncoder.getInstance().quoteAsString(filtered, output);
		output.append('"');
		return output.toString();
	}
	
	protected byte[] escapeAsJsonTextValue(byte[] filtered) {
		// TODO slow, but usually not on critical path
		String escaped = escapeAsJsonTextValue(new String(filtered, StandardCharsets.UTF_8));
		
		return escaped.getBytes(StandardCharsets.UTF_8);
	}

	protected byte[] handleInvalidBodyAsBytes(byte[] bodyAsString) {
		// escape as string
		if(compact) {
			bodyAsString = compact(bodyAsString);
		}

		return escapeAsJsonTextValue(bodyAsString);
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
    
    protected byte[] compact(byte[] json) {
    	for(int i = 0; i < json.length; i++) {
    		if(json[i] == '\n') {
    			String escaped = compact(new String(json, StandardCharsets.UTF_8));

    			return escaped.getBytes(StandardCharsets.UTF_8);
    		}
    	}
    	return json;
    }
    
    protected String compact(String json) {
        return json.replace("\n", "");
    }
}
