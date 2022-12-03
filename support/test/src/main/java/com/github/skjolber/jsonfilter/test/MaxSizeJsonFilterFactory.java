package com.github.skjolber.jsonfilter.test;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.test.truth.JsonFilterUnitTest;

public class MaxSizeJsonFilterFactory {

	private static final String[] SPACES;
	
	static {
		SPACES = new String[128];
		
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < SPACES.length; i++) {
			SPACES[i] = b.toString();
			b.append(' ');
		}
	}
	
	private MaxSizeJsonFilterAdapter adapter;
	
	public MaxSizeJsonFilterFactory(MaxSizeJsonFilterAdapter adapter) {
		this.adapter = adapter;
	}
	
	public JsonFilterUnitTest filter(String input, int expectedLength) {
		if(expectedLength < input.length()) {
			input = spaces(input, expectedLength - input.length() + 1);
		}		
	
		JsonFilter jsonFilter = adapter.getMaxSize(expectedLength);
		
		String process = jsonFilter.process(input);
		
		return JsonFilterUnitTest.newBuilder()
				.withFilter(jsonFilter)
				.withInputFile(path)
				.withOutputFile(path)
				.withOutputProperties(properties)
				.build();
		
	}


	private String spaces(String content, int length) {
		StringBuilder builder = new StringBuilder(content.length() + length);
		builder.append(content);
		for(int i = 0; i < length; i++) {
			builder.append(' ');
		}
		
		return builder.toString();
	}

}
