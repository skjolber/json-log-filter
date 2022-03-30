package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class DefaultJacksonJsonFilter extends AbstractJsonFilter {

	protected final JsonFactory jsonFactory;

	public DefaultJacksonJsonFilter() {
		this(new JsonFactory());
	}

	public DefaultJacksonJsonFilter(JsonFactory jsonFactory) {
		super(-1, -1, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
		this.jsonFactory = jsonFactory;
	}
	
	protected DefaultJacksonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
		
		this.jsonFactory = jsonFactory;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		try (JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			if(parse(parser)) {
				output.ensureCapacity(output.length() + length);
				output.append(chars, offset, length);
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
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
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		try (JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			if(parse(parser)) {
				output.ensureCapacity(output.length() + length);
				
				char[] buffer = new char[4 * 1024];
				
				InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(bytes, offset, length), StandardCharsets.UTF_8);

				int read;
				do {
					read = isr.read(buffer);
					if(read == -1) {
						break;
					}
					output.append(buffer, 0, read);
				} while(true);
				
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
		}
		
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, OutputStream output) {
		try (JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			if(parse(parser)) {
				output.write(chars, offset, length);
				return true;
			}
			return false;
		} catch(final Exception e) {
			return false;
		}
	}	

	protected boolean parse(JsonParser parser) {
		try {
			while(parser.nextToken() != null);
			
			return true;
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close
		
		return true;
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