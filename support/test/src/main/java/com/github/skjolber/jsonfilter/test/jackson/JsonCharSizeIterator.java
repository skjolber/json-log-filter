package com.github.skjolber.jsonfilter.test.jackson;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonItem;

/**
 * 
 * Iterator which essentially substrings the input document along its parsed JSON events.
 * Produces output like
 * <pre>
 * {@code
 * {}
 * {"firstName":"John"}
 * {"firstName":"John","lastName":"Smith"}
 * {"firstName":"John","lastName":"Smith","isAlive":true}
 * ...
 * }
 * </pre>
 * until the full document is output. 
 */

public class JsonCharSizeIterator implements Iterator<MaxSizeJsonItem> {

	private final JsonFactory jsonFactory;
	private final String input;
	private int events = 0;
	private MaxSizeJsonItem next;
	private boolean inputExhausted = false;

	public JsonCharSizeIterator(String input) {
		this.jsonFactory = new JsonFactory();
		this.input = input;

	}

	public JsonCharSizeIterator(JsonFactory jsonFactory, String input) {
		this.jsonFactory = jsonFactory;
		this.input = input;
	}
	
	@Override
	public boolean hasNext() {
		do {
			MaxSizeJsonItem item = nextImpl();
			if(item != null) {
				if(next == null || item.getContentAsString().length() > next.getContentAsString().length()) {
					next = item;
					return true;
				}
			}
		} while(!inputExhausted);
		
		return false;
	}

	@Override
	public MaxSizeJsonItem next() {
		return next;
	}
	
	public MaxSizeJsonItem next(PrettyPrinter prettyPrinter) {
		StringWriter bout = new StringWriter(input.length());
		
		try (
			JsonGenerator generator = jsonFactory.createGenerator(bout);
			StringReader bin = new StringReader(input);
			JsonParser jsonParser = jsonFactory.createParser(bin);
			) {
			generator.setPrettyPrinter(prettyPrinter);
			
			int count = events;
			while(jsonParser.nextToken() != null) {
				generator.copyCurrentEvent(jsonParser);
				if(jsonParser.getCurrentToken() != JsonToken.FIELD_NAME) {
					count--;
					if(count == 0) {
						generator.flush();
						int length = bout.getBuffer().length();
						
						generator.close();
						
						return new MaxSizeJsonItem(length, bout.toString());
					}
				}
			}
			generator.close();

			String string = bout.toString();
			return new MaxSizeJsonItem(string.length(), string);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public MaxSizeJsonItem nextImpl() {
		// implementation note: Not all events result in a larger document
		if(inputExhausted) {
			return null;
		}
		
		events++;

		StringWriter bout = new StringWriter(input.length());
		
		try (
			JsonGenerator generator = jsonFactory.createGenerator(bout);
			StringReader bin = new StringReader(input);
			JsonParser jsonParser = jsonFactory.createParser(bin);
			) {
			int count = events;
			while(jsonParser.nextToken() != null) {
				generator.copyCurrentEvent(jsonParser);
				if(jsonParser.getCurrentToken() != JsonToken.FIELD_NAME) {
					count--;
					if(count == 0) {
						generator.flush();
						int length = bout.getBuffer().length();
						
						generator.close();
						
						return new MaxSizeJsonItem(length, bout.toString());
					}
				}
			}
			generator.close();

			inputExhausted = true;
			
			String string = bout.toString();
			return new MaxSizeJsonItem(string.length(), string);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

}
