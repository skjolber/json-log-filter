package com.github.skjolber.jsonfilter.test.jackson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

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
			JsonGenerator generator = jsonFactory.createGenerator(bout);
			ByteArrayInputStream bin = new ByteArrayInputStream(input);
			JsonParser jsonParser = jsonFactory.createParser(bin);
			) {
			if(prettyPrint) {
				generator.useDefaultPrettyPrinter();
			}
			int count = events;
			while(count > 0 && jsonParser.nextToken() != null) {
				generator.copyCurrentEvent(jsonParser);
				if(jsonParser.getCurrentToken() != JsonToken.FIELD_NAME) {
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
