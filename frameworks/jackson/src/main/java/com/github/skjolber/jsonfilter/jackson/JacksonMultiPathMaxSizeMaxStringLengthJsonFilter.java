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
import com.github.skjolber.jsonfilter.base.path.PathItem;

public class JacksonMultiPathMaxSizeMaxStringLengthJsonFilter extends JacksonMultiPathMaxStringLengthJsonFilter implements JacksonJsonFilter {

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, anonymizes, prunes, new JsonFactory());
	}

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	protected JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	protected JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
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

		final long maxSize = this.maxSize;

		int level = 0;

		PathItem pathItem = this.pathItem;

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
				pathItem.constrain(level);
				
				level--;
			}
				
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} 
			
			if(nextToken == JsonToken.FIELD_NAME) {
				String currentName = parser.currentName();
				
				boolean prune = false;
				boolean anon = false;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level);
						
				if(pathItem.getLevel() == level) {
					pathItem = pathItem.matchPath(currentName);
					
					// match again any higher filter
					if(pathItem.hasType()) {
						// matched
						if(pathItem.getType() == FilterType.ANON) {
							anon = true;
						} else {
							prune = true;
						}
						pathItem = pathItem.constrain(level);
					}
				}
				
				if(anyElementFilters != null) {
					FilterType filterType = matchAnyElements(parser.getCurrentName());
					if(filterType == FilterType.ANON) {
						anon = true;
					} else if(filterType == FilterType.PRUNE) {
						prune = true;
					}
				}
				
				if(prune || anon) {				
					nextToken = parser.nextToken();
					if(nextToken == JsonToken.VALUE_STRING) {
						parser.getTextLength();
					} 
					
					if(nextToken.isScalarValue()) {
						char[] message;
						if(anon) {
							message = anonymizeJsonValue;
						} else {
							message = pruneJsonValue;
						}
						
						generator.writeFieldName(currentName);
						generator.writeRawValue(message, 0, message.length);
					} else {
						// array or object
						if(anon) {
							generator.writeFieldName(currentName);
							generator.copyCurrentEvent(parser);
							// keep structure, but mark all values
							if(!JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter.anonymizeChildren(parser, generator, maxSize, outputSizeSupplier, anonymizeJsonValue)) {
								break;
							}
						} else {
							if(currentName.length() + 3 + pruneJsonValue.length + outputSizeSupplier.getAsLong() >= maxSize) {
								break;
							}
							generator.writeFieldName(currentName);
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							
							parser.skipChildren(); // skip children
						}
					}
					offset = offsetSupplier.getAsLong();
					
					pathItem = pathItem.constrain(level);
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
		
		return true;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(maxSize >= length) {
			return super.process(chars, offset, length, output);
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
			return super.process(bytes, offset, length, output);
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
			return super.process(bytes, offset, length, output);
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

		final long maxSize = this.maxSize;

		int level = 0;

		PathItem pathItem = this.pathItem;

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
				pathItem.constrain(level);

				level--;
			}
				
			if(nextToken == JsonToken.VALUE_STRING) {
				parser.getTextLength();
			} 
			
			if(nextToken == JsonToken.FIELD_NAME) {
				String currentName = parser.currentName();
				
				boolean prune = false;
				boolean anon = false;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(currentName);

				if(pathItem.hasType()) {
					// matched
					if(pathItem.getType() == FilterType.ANON) {
						anon = true;
					} else {
						prune = true;
					}
					pathItem = pathItem.constrain(level);
				}
				
				if(anyElementFilters != null) {
					FilterType filterType = matchAnyElements(parser.getCurrentName());
					if(filterType == FilterType.ANON) {
						anon = true;
					} else if(filterType == FilterType.PRUNE) {
						prune = true;
					}
				}
				
				if(prune || anon) {				
					nextToken = parser.nextToken();
					if(nextToken == JsonToken.VALUE_STRING) {
						parser.getTextLength();
					} 
					
					if(nextToken.isScalarValue()) {
						char[] message;
						if(anon) {
							message = anonymizeJsonValue;
						} else {
							message = pruneJsonValue;
						}
						
						generator.writeFieldName(currentName);
						generator.writeRawValue(message, 0, message.length);
					} else {
						// array or object
						if(anon) {
							generator.writeFieldName(currentName);
							generator.copyCurrentEvent(parser);
							// keep structure, but mark all values
							if(!JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter.anonymizeChildren(parser, generator, maxSize, outputSizeSupplier, anonymizeJsonValue, metrics)) {
								break;
							}
						} else {
							if(currentName.length() + 3 + pruneJsonValue.length + outputSizeSupplier.getAsLong() >= maxSize) {
								metrics.onMaxSize(-1);

								break;
							}
							generator.writeFieldName(currentName);
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							
							parser.skipChildren(); // skip children
							
							metrics.onPrune(1);
						}
					}
					offset = offsetSupplier.getAsLong();
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
				metrics.onMaxSize(-1);

				break;
			}

			if(fieldName != null) {
				generator.writeFieldName(fieldName);
				fieldName = null;
			}

			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);

				metrics.onMaxStringLength(1);

				offset = nextOffset;

				continue;
			}
			
			generator.copyCurrentEvent(parser);
			
			offset = nextOffset;
		}
		
		return true;
	}

}