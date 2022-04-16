package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
		if(chars.length <= offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getCharOffset());
		} catch(final Exception e) {
			return false;
		}
	}
	
	protected boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
		if(bytes.length <= offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output);
		}

		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(bytes.length <= offset + length) {
			return false;
		}

		if(maxSize >= length) {
			output.append(new String(bytes, offset, length));
			return true;
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier) throws IOException {
		// measure size only based on input source offset 
		// the implementation in inaccurate to the size of an number field + max level of depth.
		
		final int maxSize = this.maxSize;

        String fieldName = null;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			// size includes field name
			long size = offsetSupplier.getAsLong();
			if(size >= maxSize) {
				break;
			}
			
			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				// preemptive size check for string value
				int length = parser.getTextLength();
				if(size + 3 + length > maxSize) {
					break;
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