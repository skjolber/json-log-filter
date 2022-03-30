package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public class JacksonMaxSizeMaxStringSizeJsonFilter extends AbstractJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;
	
	public JacksonMaxSizeMaxStringSizeJsonFilter(int maxStringLength, int maxSize) {
		this(maxStringLength, maxSize, new JsonFactory());
	}

	public JacksonMaxSizeMaxStringSizeJsonFilter(int maxStringLength, int maxSize, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxSizeMaxStringSizeJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxSizeMaxStringSizeJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage);
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
				// preemptive size check for string value
				int length = Math.min(maxStringLength, parser.getTextLength());
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
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				continue;
			}

			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, OutputStream output) {
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