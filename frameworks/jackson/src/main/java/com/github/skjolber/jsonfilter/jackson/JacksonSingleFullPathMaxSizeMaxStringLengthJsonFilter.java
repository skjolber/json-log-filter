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

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type) {
		this(maxStringLength, maxSize, maxPathMatches, expression, type, new JsonFactory());
	}

	public JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, JsonFactory jsonFactory) {
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
		if(!mustConstrainMaxSize(length)) {
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
		if(!mustConstrainMaxSize(length)) {
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
		if(!mustConstrainMaxSize(length)) {
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
		if(!mustConstrainMaxSize(length)) {
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
		if(!mustConstrainMaxSize(length)) {
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
		if(!mustConstrainMaxSize(length)) {
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

		System.out.println("Start");
		
		final String[] elementPaths = this.paths;

		int maxSize = this.maxSize;

		int level = 0;
		int matches = 0;

        String fieldName = null;
        
        long offset = offsetSupplier.getAsLong();
        
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			switch(nextToken) {
			case START_OBJECT:
				level++;
			case START_ARRAY:
				maxSize--;
				break;
			case END_OBJECT:
				level--;
				
				matches = level;
			case END_ARRAY:
				maxSize++;
				break;
			case VALUE_STRING:
				parser.getTextLength();
				break;
				default: // do nothing
			}
			
			if(nextToken == JsonToken.FIELD_NAME) {
				String currentName = parser.currentName();
				
				// reset match for a sibling field name, if any
				matches = level - 1;

				if(matches < elementPaths.length && matchPath(currentName, elementPaths[matches])) {
					matches++;
				}
				if(matches == elementPaths.length) {
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
						
						long size = currentName.length() + 3 + message.length + outputSizeSupplier.getAsLong() ;
						if(parser.getParsingContext().getCurrentIndex() >= 2) {
							size++;
						}
						
						if(size > maxSize) {
							System.out.println("4");
							break;
						}
						
						generator.writeFieldName(currentName);
						generator.writeRawValue(message, 0, message.length);
					} else {
						// array or object
						if(filterType == FilterType.ANON) {

							// the anon message might not be written
							long size = currentName.length() + 3 + outputSizeSupplier.getAsLong();
							if(parser.getParsingContext().getCurrentIndex() >= 2) {
								size++;
							}

							if(size > maxSize) {
								System.out.println("1");
								break;
							}
							
							// keep structure, but mark all values
							generator.writeFieldName(currentName);
							generator.copyCurrentEvent(parser);

							if(!anonymizeChildren(parser, generator, maxSize, outputSizeSupplier, anonymizeJsonValue)) {
								break;
							}
						} else {
							long size = currentName.length() + 3 + pruneJsonValue.length + outputSizeSupplier.getAsLong();
							if(parser.getParsingContext().getCurrentIndex() >= 2) {
								size++;
							}

							if(size > maxSize) {
								System.out.println("2");
								break;
							}
							generator.writeFieldName(currentName);
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
			
			long size = nextOffset - offset; // i.e. this includes whitespace

			long outputSize = outputSizeSupplier.getAsLong();
			
			System.out.println("MaxSize " + maxSize + " " + size + " " + outputSize);

			if(outputSize + size > maxSize) {

				// do more accurate size calculation
				int accurateSize = JacksonMaxSizeMaxStringLengthJsonFilter.getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue);
				
				System.out.println("Accurate size " + accurateSize);
				
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
				
				if(nextToken == JsonToken.START_OBJECT) {
					if(level > matches) {
						generator.copyCurrentEvent(parser);
						
						if(!JacksonMaxSizeMaxStringLengthJsonFilter.skipMaxSizeMaxStringLength(parser, generator, maxSize, maxStringLength, truncateStringValue, builder, offsetSupplier, outputSizeSupplier, metrics)) {
							System.out.println("SKIPO");
							break;
						}
						
						offset = offsetSupplier.getAsLong();
						
						maxSize++;
						
						continue;
					}
				}
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
	
	protected static boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, long maxSize, LongSupplier outputSizeSupplier, char[] anonymizeJsonValue) throws IOException {
		return anonymizeChildren(parser, generator, maxSize, outputSizeSupplier, anonymizeJsonValue, null);
	}
	
	protected static boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, long maxSize, LongSupplier outputSizeSupplier, char[] anonymizeJsonValue, JsonFilterMetrics metrics) throws IOException {
		int level = 1;
		
        String fieldName = null;
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				return false;
			}
			
			switch(nextToken) {
			case START_OBJECT:
			case START_ARRAY:
				level++;
				maxSize--;
				break;
			case END_OBJECT:
			case END_ARRAY:
				level--;
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

			long outputSize;
			if(nextToken.isScalarValue()) {
				outputSize = anonymizeJsonValue.length;
			} else {
				outputSize = 1;
			}
			if(fieldName != null) {
				outputSize += fieldName.length() + 3;
			}
			
			if(parser.getParsingContext().getCurrentIndex() >= 2) {
				outputSize++;
			}
			
			long size = outputSizeSupplier.getAsLong();
			
			if(outputSize + size > maxSize) {
				if(metrics != null) {
					metrics.onMaxSize(-1);
				}
				return false;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
			} else {
				generator.copyCurrentEvent(parser);
			}
		}

		return true;
	}


}