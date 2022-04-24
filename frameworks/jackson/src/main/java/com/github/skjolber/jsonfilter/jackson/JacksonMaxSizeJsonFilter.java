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
		if(chars.length < offset + length) {
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
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.getOutputBuffered() + output.length());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
		if(bytes.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output);
		}

		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.getOutputBuffered() + output.size());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(bytes.length < offset + length) {
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
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.getOutputBuffered() + output.length());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		final long maxSize = this.maxSize;

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			} 
			
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} 

			long nextOffset = offsetSupplier.getAsLong();
			
			long size;
			if(nextToken == JsonToken.VALUE_STRING) {
				size = parser.getTextLength() + 2;
				if(fieldName != null) {
					size += fieldName.length() + 2;
				}
			} else {
				size = nextOffset - offset; // i.e. this includes whitespace
			}
			long outputSize = outputSizeSupplier.getAsLong();

			if(outputSize + size >= maxSize) {
				break;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return true;
	}

}