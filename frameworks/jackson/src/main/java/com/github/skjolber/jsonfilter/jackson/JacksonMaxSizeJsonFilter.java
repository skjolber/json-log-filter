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

public class JacksonMaxSizeJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;
	
	public JacksonMaxSizeJsonFilter(int maxSize) {
		this(maxSize, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, JsonFactory jsonFactory) {
		this(maxSize, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxSize, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxSizeJsonFilter(int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
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
		
		final int maxSize = this.maxSize - 5; // account for 4x double quotes and a colon

        String fieldName = null;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			long size = parser.currentLocation().getCharOffset();
			if(size >= maxSize) {
				break;
			}
			
			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				if(size + fieldName.length() > maxSize) {
					break;
				}
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				int length = parser.getTextLength();
				if(fieldName != null) {
					if(size + fieldName.length() + length > maxSize) {
						break;
					}
				} else {
					if(length > maxSize) {
						break;
					}
				}
			}
			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
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