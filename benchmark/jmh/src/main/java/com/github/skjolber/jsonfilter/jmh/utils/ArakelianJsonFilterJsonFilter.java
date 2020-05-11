package com.github.skjolber.jsonfilter.jmh.utils;

import java.io.CharArrayReader;
import java.io.IOException;

import com.arakelian.json.ImmutableJsonFilterOptions;
import com.arakelian.json.JsonFilter;
import com.arakelian.json.JsonFilterOptions;
import com.arakelian.json.JsonReader;
import com.arakelian.json.JsonWriter;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.base.StringBuilderWriter;

public class ArakelianJsonFilterJsonFilter extends DefaultJsonFilter {

	// configure filter
	private final JsonFilterOptions opts;
	  
	public ArakelianJsonFilterJsonFilter(String path) {
		 opts = ImmutableJsonFilterOptions.builder() //
				  .addExcludes(path.substring(1)) //
				  .build();
	}
	
	public boolean process(final char[] chars, int offset, int length, final StringBuilder buffer) {
		
		// configure input and output streams
		JsonReader reader = new JsonReader(new CharArrayReader(chars, offset, length));
		JsonWriter<?> writer = new JsonWriter<>(new StringBuilderWriter(buffer));

		// execute filter
		JsonFilter filter = new JsonFilter(reader, writer, opts);
		try {
			filter.process();
			
			return true;
		} catch (IOException e) {
			buffer.setLength(0);
			
			return false;
		}
	}

}
