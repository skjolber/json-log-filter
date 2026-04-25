package com.github.skjolber.jsonfilter.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.json.JsonFactory;
import com.github.skjolber.jsonfilter.test.jackson.PrettyPrintWriteContext;

public class Generator {

	private static JsonFactory factory = JsonFactory.builder()
			.streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
			.streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
			.build();

	public static byte[] generateDeepObjectStructure(int levels, boolean prettyPrint) throws IOException {
		levels--;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {			
			generator.writeStartObject();
			for(int i = 0; i < levels; i++) {
				generator.writeName("f" + i);
				generator.writeStartObject();
			}

			generator.writeStringProperty("deep", "value");

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
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
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
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartArray();
			for(int i = 0; i < levels; i++) {
				if(i % 2 == 0) {
					generator.writeStartObject();
				} else {
					generator.writeName("f" + i);
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
