package com.github.skjolber.jsonfilter.jackson;
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
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
		}
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, () -> parser.currentLocation().getCharOffset(), output);
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
			return process(parser, generator, () -> parser.currentLocation().getByteOffset(), output);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(final JsonParser parser, JsonGenerator generator, LongSupplier offsetSupplier, StringBuilder output) throws IOException {
		// measure size only based on input source offset 
		// the implementation in inaccurate to the size of an number field + max level of depth.

		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		int maxSize = this.maxSize;

		final String[] elementPaths = this.paths;

		int level = 0;
		int matches = 0;
		
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
			if(nextToken == JsonToken.START_OBJECT) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT) {
				level--;

				if(matches >= level - 1) {
					matches = level - 1;
				}
			} else if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				// preemptive size check for string value
				int length = Math.min(maxStringLength, parser.getTextLength());
				if(size + 3 + length > maxSize) {
					break;
				}
			}
			
			if(fieldName != null) {
				if(matches + 1 == level && matches < elementPaths.length && matchPath(fieldName, elementPaths[matches])) {
					matches++;
				}				
				
				generator.writeFieldName(fieldName);
				fieldName = null;
				
				if(matches == elementPaths.length) {
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
							maxSize = anonymizeChildren(parser, generator, maxSize, offsetSupplier, output);
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
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
				maxSize += parser.getTextLength() - maxStringLength - truncateStringValue.length;

				continue;
			}

			generator.copyCurrentEvent(parser);
		}
		generator.flush(); // don't close

		return true;
	}
	
	protected void anonymizeChildren(JsonParser parser, JsonGenerator generator) throws IOException {
		int level = 1;

		while(level > 0) {
			JsonToken nextToken = parser.nextToken();

			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);

				continue;
			}

			generator.copyCurrentEvent(parser);
		}  				
	}
	
	public int anonymizeChildren(final JsonParser parser, JsonGenerator generator, int maxSize, LongSupplier offsetSupplier, StringBuilder output) throws IOException {
		generator.flush();
		
		int startOutputSize = output.length();
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

		int writtenSize = output.length() - startOutputSize;
		int readSize = (int)(offsetSupplier.getAsLong() - startInputSize);

		return maxSize + readSize - writtenSize;
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