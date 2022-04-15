package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class Generator {

	private static JsonFactory factory = new JsonFactory();

	public static byte[] generateDeepObjectStructure(int levels) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try (JsonGenerator generator = factory.createGenerator(bout)) {
			generator.writeStartObject();
			for(int i = 0; i < levels; i++) {
				generator.writeFieldName("field" + i);
				generator.writeStartObject();
			}

			generator.writeStringField("deep", "value");

			for(int i = 0; i < levels; i++) {
				generator.writeEndObject();
			}
			
			generator.writeEndObject();
		};
		
		return bout.toByteArray();
	}
	
	public static byte[] generateDeepArrayStructure(int levels) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try (JsonGenerator generator = factory.createGenerator(bout)) {
			generator.writeStartArray();
			for(int i = 0; i < levels; i++) {
				generator.writeString("array " + i);
				generator.writeStartArray();
			}
			
			for(int i = 0; i < levels; i++) {
				generator.writeEndArray();
			}
			
			generator.writeEndArray();
		};
		
		return bout.toByteArray();
	}
	
	public static byte[] generateDeepMixedStructure(int levels) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try (JsonGenerator generator = factory.createGenerator(bout)) {
			generator.writeStartArray();
			for(int i = 0; i < levels; i++) {
				if(i % 2 == 0) {
					generator.writeStartObject();
				} else {
					generator.writeFieldName("field" + i);
					generator.writeStartArray();
				}
			}
			
			for(int i = levels - 1; i >= 0; i--) {
				if(i % 2 == 0) {
					generator.writeEndObject();
				} else {
					generator.writeEndArray();
				}
			}
			
			generator.writeEndArray();
		};
		
		return bout.toByteArray();
	}	
}
