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
		return generateDeepObjectStructure(levels, "value", prettyPrint);
	}

	public static byte[] generateDeepObjectStructure(int levels, String leafValue, boolean prettyPrint) throws IOException {
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

			generator.writeStringProperty("deep", leafValue);

			for(int i = 0; i < levels; i++) {
				generator.writeEndObject();
			}

			generator.writeEndObject();
		} finally {
			generator.close();
		}

		return bout.toByteArray();
	}

	public static byte[] generateObjectWithDeepObjectValue(int levels, String wrapKey, String siblingKey, String siblingValue, boolean prettyPrint) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartObject();
			generator.writeName(wrapKey);
			int innerLevels = levels - 1;
			generator.writeStartObject();
			for(int i = 0; i < innerLevels; i++) {
				generator.writeName("f" + i);
				generator.writeStartObject();
			}
			generator.writeStringProperty("deep", "value");
			for(int i = 0; i < innerLevels; i++) {
				generator.writeEndObject();
			}
			generator.writeEndObject();
			generator.writeStringProperty(siblingKey, siblingValue);
			generator.writeEndObject();
		} finally {
			generator.close();
		}

		return bout.toByteArray();
	}

	/**
	 * Generates {@code {shallowKey: shallowValue, deepKey: {levels of nesting}}}.
	 * The shallow key appears first, so after a path filter matches it, the deeply
	 * nested value remains for further processing.
	 */
	public static byte[] generateObjectWithShallowKeyAndDeepValue(int levels, String shallowKey, String shallowValue, String deepKey, boolean prettyPrint) throws IOException {
		int innerLevels = levels - 1;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartObject();
			generator.writeStringProperty(shallowKey, shallowValue);
			generator.writeName(deepKey);
			generator.writeStartObject();
			for(int i = 0; i < innerLevels; i++) {
				generator.writeName("f" + i);
				generator.writeStartObject();
			}
			generator.writeStringProperty("leaf", "value");
			for(int i = 0; i < innerLevels; i++) {
				generator.writeEndObject();
			}
			generator.writeEndObject();
			generator.writeEndObject();
		} finally {
			generator.close();
		}

		return bout.toByteArray();
	}

	public static byte[] generateObjectWithDeepArrayValue(int levels, String wrapKey, String siblingKey, String siblingValue, boolean prettyPrint) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartObject();
			generator.writeName(wrapKey);
			for(int i = 0; i < levels; i++) {
				generator.writeStartArray();
			}
			generator.writeNumber(1);
			for(int i = 0; i < levels; i++) {
				generator.writeEndArray();
			}
			generator.writeStringProperty(siblingKey, siblingValue);
			generator.writeEndObject();
		} finally {
			generator.close();
		}

		return bout.toByteArray();
	}

	public static byte[] generateDeepPathWithObjectLeaf(int pathLevels, String leafKey, boolean prettyPrint) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartObject();
			for(int i = 0; i < pathLevels; i++) {
				generator.writeName("k" + i);
				generator.writeStartObject();
			}
			generator.writeName(leafKey);
			generator.writeStartObject();
			generator.writeStringProperty("inner", "value");
			generator.writeEndObject();
			for(int i = 0; i < pathLevels; i++) {
				generator.writeEndObject();
			}
			generator.writeEndObject();
		} finally {
			generator.close();
		}

		return bout.toByteArray();
	}

	public static byte[] generateDeepPathWithSiblings(int pathLevels, String objectKey, String stringKey, String stringValue, boolean prettyPrint) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		JsonGenerator generator;
		if(prettyPrint) {
			generator = factory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout);
		} else {
			generator = factory.createGenerator(bout);
		}
		try {
			generator.writeStartObject();
			for(int i = 0; i < pathLevels; i++) {
				generator.writeName("k" + i);
				generator.writeStartObject();
			}
			generator.writeName(objectKey);
			generator.writeStartObject();
			generator.writeStringProperty("x", "v");
			generator.writeEndObject();
			generator.writeStringProperty(stringKey, stringValue);
			for(int i = 0; i < pathLevels; i++) {
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
