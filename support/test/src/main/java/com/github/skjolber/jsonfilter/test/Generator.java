package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class Generator {

	private static JsonFactory factory = new JsonFactory();

	public static byte[] generateDeepObjectStructure(int levels, boolean prettyPrint) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(bout).useDefaultPrettyPrinter();
		} else {
			generator = factory.createGenerator(bout);
		}
		try {			
			generator.writeStartObject();
			for(int i = 0; i < levels; i++) {
				generator.writeFieldName("f" + i);
				generator.writeStartObject();
			}

			generator.writeStringField("deep", "value");

			for(int i = 0; i < levels; i++) {
				generator.writeEndObject();
			}
			
			generator.writeEndObject();
		} finally {
			generator.close();
		}
		
		return bout.toByteArray();
	}
	
	public static byte[] generateDeepArrayStructure(int levels, boolean prettyPrint) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(bout).useDefaultPrettyPrinter();
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartArray();
			for(int i = 0; i < levels; i++) {
				generator.writeString("a" + i);
				generator.writeStartArray();
			}
			
			for(int i = 0; i < levels; i++) {
				generator.writeEndArray();
			}
			
			generator.writeEndArray();
		} finally {
			generator.close();
		}
		
		return bout.toByteArray();
	}
	
	public static byte[] generateDeepMixedStructure(int levels, boolean prettyPrint) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(bout).useDefaultPrettyPrinter();
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartArray();
			for(int i = 0; i < levels; i++) {
				if(i % 2 == 0) {
					generator.writeStartObject();
				} else {
					generator.writeFieldName("f" + i);
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
		} finally {
			generator.close();
		}
		
		return bout.toByteArray();
	}	
}
