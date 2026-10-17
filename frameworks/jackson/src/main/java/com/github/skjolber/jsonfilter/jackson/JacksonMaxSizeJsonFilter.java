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

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(chars.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(chars, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, length, metrics)) {
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
	
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}

		int outputSize = output.size();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.size() - outputSize, length, metrics)) {
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
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(bytes.length < offset + length) {
			return false;
		}

		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, length, metrics)) {
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

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
		return process(parser, generator, offsetSupplier, outputSizeSupplier, -1, metrics);
	}

	/**
	 * Filter with max size.
	 *
	 * @param parser parser
	 * @param generator generator
	 * @param offsetSupplier input offset, relative to the start of the input
	 * @param outputSizeSupplier output written so far, in characters or bytes
	 * @param inputLength input length in characters or bytes, for the max size metric, or -1 if unknown
	 * @param metrics metrics, or null
	 * @return true if the input was a single root value
	 * @throws IOException if the input could not be read or the output written
	 */

	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, int inputLength,
			JsonFilterMetrics metrics) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		
		// TODO optimize while output size < 50%, simplify copy operation

		int maxSize = this.maxSize;

		boolean utf8 = JacksonOutputSize.isUtf8(generator);

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
				
				int accurateSize = JacksonOutputSize.getAccurateSize(parser, fieldName, nextToken, utf8);

				// a scalar root value is kept even if too big; dropping it would leave an empty (invalid) document
				if(outputSize + accurateSize > maxSize && !parser.streamReadContext().inRoot()) {
					if(metrics != null) {
						metrics.onMaxSize(JacksonOutputSize.getLeftOutSize(inputLength, offset));
					}
					
					break;
				}
			}

			if(fieldName != null) {
				generator.writeName(fieldName);
				fieldName = null;
			}

			JacksonEvents.copyCurrentEvent(parser, generator);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return JacksonEvents.isSingleRootValue(parser);
	}

	protected static int getAccurateSize(final JsonParser parser, String fieldName, JsonToken nextToken) throws IOException {
		return JacksonOutputSize.getAccurateSize(parser, fieldName, nextToken, false);
	}

}
