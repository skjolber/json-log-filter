package com.github.skjolber.jsonfilter.jackson;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jsonfilter.JsonFilter;

public interface JacksonJsonFilter extends JsonFilter {

	boolean process(InputStream in, JsonGenerator generator) throws Exception;
	
	default boolean process(byte[] chars, JsonGenerator generator) throws Exception {
		return process(chars, 0, chars.length, generator);
	}

	boolean process(byte[] chars, int offset, int length, JsonGenerator generator) throws Exception;

	default boolean process(char[] chars, JsonGenerator generator) throws Exception {
		return process(chars, 0, chars.length, generator);
	}

	boolean process(char[] chars, int offset, int length, JsonGenerator generator) throws Exception;

	boolean process(final JsonParser parser, JsonGenerator generator) throws Exception;
}
