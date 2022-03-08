package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class Generator {

	private static JsonFactory factory = new JsonFactory();

	public static byte[] generateDeepStructure(int levels) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try (JsonGenerator generator = factory.createGenerator(bout)) {
			generator.writeStartObject();
			for(int i = 0; i < levels; i++) {
				generator.writeFieldName("field" + i);
				generator.writeStartObject();
			}
			
			for(int i = 0; i < levels; i++) {
				generator.writeEndObject();
			}
			
			generator.writeEndObject();
		};
		
		return bout.toByteArray();
	}
}
