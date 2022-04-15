package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonMaxSizeJsonFilter extends DefaultJacksonJsonFilter implements JacksonJsonFilter {

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
		super(-1, maxSize, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(maxSize >= length) {
			super.process(chars, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(maxSize >= length) {
			super.process(bytes, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(bytes, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		byte[] chars = new byte[4 * 1024];

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int read;
		do {
			read = in.read(chars, 0, chars.length);
			if(read == -1) {
				break;
			}
			
			bout.write(chars, 0, read);
		} while(true);

		byte[] bytes = bout.toByteArray();
		
		return process(bytes, 0, bytes.length, generator);
	}

	public boolean process(byte[] bytes, int offset, int length, JsonGenerator generator) {
		if(maxSize >= length) {
			super.process(bytes, offset, length, generator);
		}
		try (final JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			return process(parser, generator, () -> parser.getCurrentLocation().getByteOffset());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) {
		if(maxSize >= length) {
			super.process(chars, offset, length, generator);
		}
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator, () -> parser.getCurrentLocation().getCharOffset());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier) throws IOException {		
		final int maxSize = this.maxSize; // account for 4x double quotes and a colon

        String fieldName = null;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			long size = offsetSupplier.getAsLong();
			if(size >= maxSize) {
				break;
			}
			
			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				if(size > maxSize) {
					break;
				}
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				// preemptive size check for string value
				int length = parser.getTextLength();
				if(fieldName != null) {
					if(size + fieldName.length() + length + 5 > maxSize) {
						break;
					}
				} else {
					if(size + 3 + length > maxSize) {
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
	public boolean process(byte[] chars, int offset, int length, OutputStream output) {
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
		}
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