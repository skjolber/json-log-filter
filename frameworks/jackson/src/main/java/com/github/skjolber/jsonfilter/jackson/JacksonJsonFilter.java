package com.github.skjolber.jsonfilter.jackson;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public interface JacksonJsonFilter extends JsonFilter {

	default boolean process(byte[] bytes, StringBuilder output) {
		return process(bytes, 0, bytes.length, output, null);
	}

	default boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		return process(bytes, offset, length, output, null);
	}
	
	default boolean process(byte[] bytes, StringBuilder output, JsonFilterMetrics filterMetrics) {
		return process(bytes, 0, bytes.length, output, filterMetrics);
	}

	boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics);
	
	public default boolean isValidating() {
		return true;
	}
	
	default boolean isRemovingWhitespace() {
		return true;
	}
	
	public default boolean isRemovingLinebreaksInStrings() {
		return true;
	}
}
