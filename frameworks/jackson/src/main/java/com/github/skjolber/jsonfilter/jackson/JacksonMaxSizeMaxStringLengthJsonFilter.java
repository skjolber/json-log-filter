
package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class JacksonMaxSizeMaxStringLengthJsonFilter extends JacksonMaxStringLengthJsonFilter implements JacksonJsonFilter {

	// size of the truncate message in UTF-8 bytes; in characters it is its length
	private final int truncateStringValueUtf8Size;

	public JacksonMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize) {
		this(maxStringLength, maxSize, new JsonFactory());
	}

	public JacksonMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);

		this.truncateStringValueUtf8Size = JacksonOutputSize.getRawSize(truncateStringValue, true);
	}
	
	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(chars, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.length() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}
	
	@Override
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.length() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}
	
	@Override
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
	
		int outputSize = output.size();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.size() - outputSize, metrics)) {
				if(metrics != null) {
					// writes buffered output and closes open structures, so the output size is final
					generator.close();
					metrics.onInput(length);
					metrics.onOutput(output.size() - outputSize);
				}
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setSize(outputSize);
		return false;
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		StringBuilder builder = null; // allocated when the first value is truncated

		int maxSize = this.maxSize;

		boolean utf8 = JacksonOutputSize.isUtf8(generator);
		int truncateSize = utf8 ? truncateStringValueUtf8Size : truncateStringValue.length;

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			switch(nextToken) {
			case START_ARRAY:
			case START_OBJECT:
				maxSize--;
				break;
			case END_ARRAY:
			case END_OBJECT:
				maxSize++;
				break;
			case PROPERTY_NAME:
				fieldName = parser.currentName();
				continue;
			case VALUE_STRING:
				parser.getTextLength();
				break;
				default: // do nothing
			}

			long nextOffset = offsetSupplier.getAsLong();
			
			long size = nextOffset - offset; // i.e. this includes whitespace

			long outputSize = outputSizeSupplier.getAsLong();

			if(outputSize + size > maxSize) {

				// do more accurate size calculation
				int accurateSize = JacksonOutputSize.getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue, truncateSize, utf8);
				
				// a scalar root value is kept even if too big; dropping it would leave an empty (invalid) document
				if(outputSize + accurateSize > maxSize && !parser.streamReadContext().inRoot()) {
					if(metrics != null) {
						metrics.onMaxSize(-1);
					}
					
					break;
				}
			}			

			if(fieldName != null) {
				generator.writeName(fieldName);
				fieldName = null;
			}

			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				if(builder == null) {
					builder = JacksonMaxStringLengthJsonFilter.newTruncationBuilder(maxStringLength, truncateStringValue);
				}
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);

				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
				
				offset = nextOffset;

				continue;
			}
			
			JacksonEvents.copyCurrentEvent(parser, generator);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return JacksonEvents.isSingleRootValue(parser);
	}

	protected static int getAccurateMaxStringSize(final JsonParser parser, String fieldName, JsonToken nextToken, int maxStringLength, char[] truncateStringValue) throws IOException {
		return JacksonOutputSize.getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue, truncateStringValue.length, false);
	}

}
