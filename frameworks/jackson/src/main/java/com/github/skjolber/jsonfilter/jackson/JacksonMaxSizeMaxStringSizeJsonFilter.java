
package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonMaxSizeMaxStringSizeJsonFilter extends JacksonMaxStringLengthJsonFilter implements JacksonJsonFilter {

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
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> output.length());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> output.length());
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement

		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		long maxOffset = this.maxSize;

        String fieldName = null;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				// offset includes field name, but output does not
				long offset = offsetSupplier.getAsLong();
				if(offset >= maxOffset) {
					maxOffset = getMaxOffset(generator, offsetSupplier, outputSizeSupplier) - 5 - fieldName.length();
					if(offset >= maxOffset) {
						break;
					}
				}
				
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				int length = Math.min(maxStringLength, parser.getTextLength());

				long offset = offsetSupplier.getAsLong();
				if(offset + 2 + length >= maxOffset) {
					maxOffset = getMaxOffset(generator, offsetSupplier, outputSizeSupplier);
					if(offset + 3 + length >= maxOffset) {
						break;
					}
				}
			} else {
				long offset = offsetSupplier.getAsLong();
				if(offset >= maxOffset) {
					maxOffset = getMaxOffset(generator, offsetSupplier, outputSizeSupplier) - JacksonMaxSizeJsonFilter.getTokenSize(parser, nextToken);
					if(offset >= maxOffset) {
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
				
				maxOffset += parser.getTextLength() - maxStringLength - truncateStringValue.length;
				
				continue;
			}

			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}
	
	protected long getMaxOffset(JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		generator.flush(); // don't close

		long outputSize = outputSizeSupplier.getAsLong();
		
		long left = this.maxSize - outputSize;

		return offsetSupplier.getAsLong() + left;
	}
}