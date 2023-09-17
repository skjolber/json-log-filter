
package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;

public class JacksonMaxSizeMaxStringLengthJsonFilter extends JacksonMaxStringLengthJsonFilter implements JacksonJsonFilter {

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
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.getOutputBuffered() + output.length());
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
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.getOutputBuffered() + output.length());
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
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
	
	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		return process(parser, generator, offsetSupplier, outputSizeSupplier, null);
	}
	
	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(chars, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.getOutputBuffered() + output.length(), metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.getOutputBuffered() + output.length(), metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(bytes, offset, length, output, metrics);
		}
	
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.getOutputBuffered() + output.size(), metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		int maxSize = this.maxSize;

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
			case FIELD_NAME:
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
				int accurateSize = getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue);
				
				if(outputSize + accurateSize > maxSize) {
					if(metrics != null) {
						metrics.onMaxSize(-1);
					}
					
					break;
				}
			}			

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);

				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
				
				offset = nextOffset;

				continue;
			}
			
			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return true;
	}

	protected static int getAccurateMaxStringSize(final JsonParser parser, String fieldName, JsonToken nextToken, int maxStringLength, char[] truncateStringValue) throws IOException {
		int accurateSize;
		if(fieldName != null) {
			accurateSize = fieldName.length() + 2;
		} else {
			accurateSize = 0;
		}
		
		if(parser.getParsingContext().getCurrentIndex() >= 2) {
			accurateSize++;
		}
		
		if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
			
			char[] textCharacters = parser.getTextCharacters();
			int textOffset = parser.getTextOffset();
			
			// A high surrogate precedes a low surrogate.
			// check last include character

			int keepLength;
			if(Character.isLowSurrogate(textCharacters[textOffset + maxStringLength])) {
				keepLength = maxStringLength - 1;
			} else {
				keepLength = maxStringLength;
			}
			
			int removeLength = parser.getTextLength() - keepLength;

			int actualReductionLength = removeLength - truncateStringValue.length - lengthToDigits(removeLength);
			if(actualReductionLength > 0) {
				accurateSize += parser.getTextLength() - actualReductionLength + 2;
			} else {
				accurateSize += parser.getTextLength() + 2;
			}
		} else {
			
			switch(nextToken) {
			case VALUE_STRING: {
				accurateSize += parser.getTextLength() + 2;
				break;
			}
			case VALUE_NUMBER_FLOAT:
			case VALUE_NUMBER_INT: {
				accurateSize += parser.getNumberValue().toString().length();
				break;
			}
			case VALUE_TRUE: 
			case VALUE_NULL: {
				accurateSize += 4;
				break;
			}
			case VALUE_FALSE: {
				accurateSize += 5;
				break;
			}
			}
		}
		return accurateSize;
	}
	
	public static boolean skipMaxSizeMaxStringLength(final JsonParser parser, JsonGenerator generator, int maxSize, int maxStringLength, char[] truncateStringValue, StringBuilder builder, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier, JsonFilterMetrics metrics) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
        int level = 1;
        
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			switch(nextToken) {
			case START_ARRAY:
			case START_OBJECT:
				maxSize--;
				level++;
				break;
			case END_ARRAY:
			case END_OBJECT:
				maxSize++;
				level--;
				break;
			case FIELD_NAME:
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
				int accurateSize = getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue);
				
				if(outputSize + accurateSize > maxSize) {
					if(metrics != null) {
						metrics.onMaxSize(-1);
					}
					
					return false;
				}
			}			

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);

				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
				
				offset = nextOffset;

				continue;
			}
			
			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return true;
	}

	
}