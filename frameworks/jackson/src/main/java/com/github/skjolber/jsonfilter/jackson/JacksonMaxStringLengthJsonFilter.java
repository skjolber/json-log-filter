package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonMaxStringLengthJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonMaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, new JsonFactory());
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, JsonFactory jsonFactory) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
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
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(bytes, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] bytes, int offset, int length, JsonGenerator generator) {
		try (final JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				String text = parser.getText();
				
				// A high surrogate precedes a low surrogate.
				// check last include character
				builder.append('"');

				int max;
				if(Character.isLowSurrogate(text.charAt(maxStringLength))) {
					max = maxStringLength - 1;
				} else {
					max = maxStringLength;
				}

				quoteAsString(text.substring(0, max), builder);
				builder.append(truncateStringValue);
				builder.append(text.length() - max);
				builder.append('"');
				
				generator.writeRawValue(builder.toString());
				builder.setLength(0);
				
				continue;
			}
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		//output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(output)) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}	

	protected char[] getPruneJsonValue() {
		return pruneJsonValue;
	}
	
	protected char[] getAnonymizeJsonValue() {
		return anonymizeJsonValue;
	}
	
	protected char[] getTruncateStringValue() {
		return truncateStringValue;
	}		
}