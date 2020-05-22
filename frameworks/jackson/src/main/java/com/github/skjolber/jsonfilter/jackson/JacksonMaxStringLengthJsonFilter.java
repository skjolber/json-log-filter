package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class JacksonMaxStringLengthJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonMaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, new JsonFactory());
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, JsonFactory jsonFactory) {
		super(maxStringLength);
		this.jsonFactory = jsonFactory;
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] chars, int offset, int length, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				String text = parser.getText();
				
				// A high surrogate precedes a low surrogate.
				// check last include character
				if(Character.isHighSurrogate(text.charAt(maxStringLength - 1))) {
					generator.writeString(text.substring(0, maxStringLength - 1) + CharArrayRangesFilter.FILTER_TRUNCATE_MESSAGE + (text.length() - maxStringLength + 1));
				} else {
					generator.writeString(text.substring(0, maxStringLength) + CharArrayRangesFilter.FILTER_TRUNCATE_MESSAGE + (text.length() - maxStringLength));
				}
				
				continue;
			}
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}	

}