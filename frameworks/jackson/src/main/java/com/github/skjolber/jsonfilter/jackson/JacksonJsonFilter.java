package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;

public interface JacksonJsonFilter extends JsonFilter {

	default boolean process(byte[] bytes, StringBuilder output) {
		return process(bytes, 0, bytes.length, output);
	}

	boolean process(byte[] bytes, int offset, int length, StringBuilder output);
	
	public default boolean isCompacting() {
		return true;
	}

	public default boolean isValidating() {
		return true;
	}
}
