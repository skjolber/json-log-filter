package com.github.skjolber.jsonfilter.jmh.utils;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.StringBuilderWriter;

import com.arakelian.json.ImmutableJsonFilterOptions;
import com.arakelian.json.JsonFilter;
import com.arakelian.json.JsonFilterOptions;
import com.arakelian.json.JsonReader;
import com.arakelian.json.JsonWriter;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;

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
	
	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(chars, offset, length), StandardCharsets.UTF_8));
		JsonWriter<?> writer = new JsonWriter<>(new OutputStreamWriter(output));

		// execute filter
		JsonFilter filter = new JsonFilter(reader, writer, opts);
		try {
			filter.process();
			
			return true;
		} catch (IOException e) {
			output.reset();
			
			return false;
		}
	}

}
