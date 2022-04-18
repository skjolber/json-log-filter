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
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> output.length());
		} catch(final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(bytes.length < offset + length) {
			return false;
		}
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
	
	protected boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
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
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> output.size());
		} catch(final Exception e) {
			return false;
		}
	}
	
	
	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		// estimate output size based on input size
		// if size limit is reached, do a more accurate output measurement

		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		long maxOffset = this.maxSize;

		final String[] elementPaths = this.paths;

		int level = 0;
		int matches = 0;
		
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
			
			if(nextToken == JsonToken.START_OBJECT) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT) {
				level--;

				if(matches >= level - 1) {
					matches = level - 1;
				}
			} else if(nextToken == JsonToken.FIELD_NAME) {
				if(matches + 1 == level && matches < elementPaths.length && matchPath(parser.currentName(), elementPaths[matches])) {
					matches++;
				}				
				if(matches == elementPaths.length) {
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					
					long size = offsetSupplier.getAsLong();
					if(nextToken.isScalarValue()) {
						char[] message;
						if(filterType == FilterType.ANON) {
							message = anonymizeJsonValue;
						} else {
							message = pruneJsonValue;
						}
						generator.writeRawValue(message, 0, message.length);

						// adjust max size
						if(nextToken == JsonToken.VALUE_STRING) {
							maxSize += parser.getTextLength() - message.length;
						}
					} else {
						// array or object
						if(filterType == FilterType.ANON) {
							generator.copyCurrentEvent(parser);

							// keep structure, but mark all values
							maxSize = anonymizeChildren(parser, generator, maxSize, offsetSupplier, outputSizeSupplier, anonymizeJsonValue);
							if(maxSize == -1) {
								break;
							}
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							
							parser.skipChildren(); // skip children
							
							maxSize += offsetSupplier.getAsLong() - size - pruneJsonValue.length;
						}
					}

					matches--;
					
					continue;
				}
			} else if(nextToken == JsonToken.VALUE_STRING) {
				// preemptive size check for string value
				int length = Math.min(maxStringLength, parser.getTextLength());
				if(size + 3 + length > maxSize) {
					break;
				}

				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
				maxSize += parser.getTextLength() - maxStringLength - truncateStringValue.length;

				continue;
			}

			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}
	
	protected static int anonymizeChildren(final JsonParser parser, JsonGenerator generator, int maxSize, LongSupplier offsetSupplier, LongSupplier output, char[] anonymizeJsonValue) throws IOException {
		generator.flush();
		
		long startOutputSize = output.getAsLong();
		long startInputSize = offsetSupplier.getAsLong();
		
		int level = 1;
		
        String fieldName = null;
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			long size = offsetSupplier.getAsLong();
			if(size >= maxSize) {
				return -1;
			}
			
			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			}

			if(nextToken.isScalarValue()) {
				if(size + anonymizeJsonValue.length > maxSize) {
					return -1;
				}
				
				if(fieldName != null) {
					generator.writeFieldName(fieldName);
					fieldName = null;
				}
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
			} else {
				if(fieldName != null) {
					generator.writeFieldName(fieldName);
					fieldName = null;
				}
				generator.copyCurrentEvent(parser);
			}
		}
		
		generator.flush(); // don't close

		long writtenSize = output.getAsLong() - startOutputSize;
		long readSize = offsetSupplier.getAsLong() - startInputSize;

		return maxSize + (int)(readSize - writtenSize);
	}

	protected long getMaxOffset(JsonGenerator generator, LongSupplier offsetSupplier, LongSupplier outputSizeSupplier) throws IOException {
		generator.flush(); // don't close

		long outputSize = outputSizeSupplier.getAsLong();
		
		long left = this.maxSize - outputSize;

		return offsetSupplier.getAsLong() + left;
	}
}