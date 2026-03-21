package com.github.skjolber.jsonfilter.test.jackson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

public class JsonByteSizeIterator implements Iterator<byte[]> {

	private final JsonFactory jsonFactory;
	private final byte[] input;
	private int events = 0;
	private boolean next = true;
	private boolean prettyPrint;

	public JsonByteSizeIterator(byte[] input, boolean prettyPrint) {
		this.jsonFactory = new JsonFactory();
		this.input = input;
		this.prettyPrint = prettyPrint;
	}

	public JsonByteSizeIterator(JsonFactory jsonFactory, byte[] input, boolean prettyPrint) {
		this.jsonFactory = jsonFactory;
		this.input = input;
		this.prettyPrint = prettyPrint;
	}
	
	@Override
	public boolean hasNext() {
		return next;
	}

	@Override
	public byte[] next() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(input.length);
		
		events++;
		
		try (
			JsonGenerator generator = prettyPrint
					? jsonFactory.createGenerator(PrettyPrintWriteContext.DEFAULT, bout)
					: jsonFactory.createGenerator(bout);
			ByteArrayInputStream bin = new ByteArrayInputStream(input);
			JsonParser jsonParser = jsonFactory.createParser(bin);
			) {
			int count = events;
			while(count > 0 && jsonParser.nextToken() != null) {
				generator.copyCurrentEvent(jsonParser);
				if(jsonParser.currentToken() != JsonToken.PROPERTY_NAME) {
					count--;
					
					if(count == 0) {
						generator.close();
						
						return bout.toByteArray();						
					}
				}
			}
			generator.close();
			
			next = false;
			
			return bout.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

}
