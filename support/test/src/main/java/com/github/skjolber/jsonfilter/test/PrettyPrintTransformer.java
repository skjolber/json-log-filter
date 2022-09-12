package com.github.skjolber.jsonfilter.test;

import java.io.CharArrayWriter;
import java.io.StringReader;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class PrettyPrintTransformer implements Function<String, String>{

	protected final JsonFactory jsonFactory = new JsonFactory();

	@Override
	public String apply(String t) {
		try (
			StringReader reader = new StringReader(t);
			CharArrayWriter writer = new CharArrayWriter();

			JsonGenerator generator = jsonFactory.createGenerator(writer);
			JsonParser parser = jsonFactory.createParser(reader)
			) {
			
			while(true) {
				JsonToken nextToken = parser.nextToken();
				if(nextToken == null) {
					break;
				}
				generator.copyCurrentEvent(parser);
			}
			
			generator.flush(); // don't close
			
			return writer.toString();
		} catch(final Exception e) {
			return t;
		}
		
		
	}

}
