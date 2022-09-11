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

public class JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter extends JacksonSingleFullPathMaxStringLengthJsonFilter implements JacksonJsonFilter {

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type) {
		this(maxStringLength, maxSize, expression, type, new JsonFactory());
	}

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, expression, type,  pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, -1, expression, type, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	protected JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
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
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		final String[] elementPaths = this.paths;

		long maxSize = this.maxSize;

		int level = 0;
		int matches = 0;

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			if(nextToken == JsonToken.START_OBJECT) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT) {
				level--;
			}
				
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} 
			
			if(nextToken == JsonToken.FIELD_NAME) {
				String currentName = parser.currentName();
				if(matches + 1 == level && matches < elementPaths.length && matchPath(currentName, elementPaths[matches])) {
					matches++;
				}				
				if(matches == elementPaths.length) {
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					if(nextToken == JsonToken.VALUE_STRING) {
						parser.getTextLength();
					} 
					
					if(nextToken.isScalarValue()) {
						char[] message;
						if(filterType == FilterType.ANON) {
							message = anonymizeJsonValue;
						} else {
							message = pruneJsonValue;
						}
						
						if(level + currentName.length() + 3 + message.length + outputSizeSupplier.getAsLong() >= maxSize) {
							break;
						}
						
						generator.writeRawValue(message, 0, message.length);
					} else {
						// array or object
						if(filterType == FilterType.ANON) {
							// keep structure, but mark all values
							generator.copyCurrentEvent(parser);

							if(!anonymizeChildren(parser, generator, maxSize, outputSizeSupplier, anonymizeJsonValue)) {
								break;
							}
						} else {
							if(level + currentName.length() + 3 + pruneJsonValue.length + outputSizeSupplier.getAsLong() >= maxSize) {
								break;
							}
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							
							parser.skipChildren(); // skip children
						}
					}
					offset = offsetSupplier.getAsLong();

					matches--;
				} else {
					fieldName = currentName;
				}
				
				continue;
			} 
			
			long nextOffset = offsetSupplier.getAsLong();
			
			long size;
			
			if(nextToken == JsonToken.VALUE_STRING) {
				size = 2;
				if(fieldName != null) {
					size += fieldName.length() + 2;
				}
				if(parser.getTextLength() > maxStringLength) {
					size += maxStringLength + truncateStringValue.length + JacksonMaxSizeMaxStringSizeJsonFilter.lengthToDigits(parser.getTextLength() - maxStringLength);
				} else {
					size += parser.getTextLength();
				}
			} else {
				size = nextOffset - offset; // i.e. this includes whitespace
			}
			
			long outputSize = outputSizeSupplier.getAsLong();
			
			if(outputSize + size + level > maxSize) {
				break;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
				offset = nextOffset;

				continue;
			}
			
			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		generator.flush();
		
		return true;
	}
	
	protected static boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, long maxSize, LongSupplier outputSizeSupplier, char[] anonymizeJsonValue) throws IOException {
		int level = 1;
		
        String fieldName = null;
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				return false;
			}
			
			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			}

			long outputSize;
			if(nextToken.isScalarValue()) {
				outputSize = anonymizeJsonValue.length;
			} else {
				outputSize = 1;
			}
			if(fieldName != null) {
				outputSize += fieldName.length() + 3;
			}
			
			long size = outputSizeSupplier.getAsLong();
			if(outputSize + size + level >= maxSize) {
				return false;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
			} else {
				generator.copyCurrentEvent(parser);
			}
		}

		return true;
	}
	
	protected static boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, long maxSize, LongSupplier outputSizeSupplier, char[] anonymizeJsonValue, JsonFilterMetrics metrics) throws IOException {
		int level = 1;
		
        String fieldName = null;
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				return false;
			}
			
			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			}

			long outputSize;
			if(nextToken.isScalarValue()) {
				outputSize = anonymizeJsonValue.length;
			} else {
				outputSize = 1;
			}
			if(fieldName != null) {
				outputSize += fieldName.length() + 3;
			}
			
			long size = outputSizeSupplier.getAsLong();
			if(outputSize + size + level >= maxSize) {
				metrics.onMaxSize(-1);
				return false;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
				
				metrics.onAnonymize(1);
			} else {
				generator.copyCurrentEvent(parser);
			}
		}

		return true;
	}


}