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
import com.github.skjolber.jsonfilter.base.path.PathItem;

/**
 * Jackson filter for paths, max string length and max size.
 * <br>
 * Prune and anonymize messages can be longer than the values they replace. So if the input fits within max size,
 * it is first filtered without max size, and filtered again with max size if the output turns out too large.
 * The metrics then include both passes.
 */

public class JacksonPathMaxSizeMaxStringLengthJsonFilter extends JacksonPathMaxStringLengthJsonFilter implements JacksonJsonFilter {

	public JacksonPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, anonymizes, prunes, new JsonFactory());
	}

	public JacksonPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	protected JacksonPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	protected JacksonPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			// prune and anonymize messages can be longer than the values they replace,
			// so check the output size, and filter with max size if it is too large.
			// The metrics then include both passes.
			int start = output.length();
			if(!super.process(chars, offset, length, output, metrics)) {
				return false;
			}
			if(!mustConstrainMaxSize(output.length() - start)) {
				return true;
			}
			output.setLength(start);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getCharOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
				return true;
			}
		} catch(final Exception e) {
			// fall through
		}
		// leave the output as it was
		output.setLength(outputSize);
		return false;
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if(!mustConstrainMaxSize(length)) {
			// prune and anonymize messages can be longer than the values they replace,
			// so check the output size, and filter with max size if it is too large.
			// The metrics then include both passes.
			int start = output.length();
			if(!super.process(bytes, offset, length, output, metrics)) {
				return false;
			}
			if(!mustConstrainMaxSize(output.length() - start)) {
				return true;
			}
			output.setLength(start);
		}
		output.ensureCapacity(output.length() + length);

		int outputSize = output.length();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.length() - outputSize, metrics)) {
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
		if(!mustConstrainMaxSize(length)) {
			// prune and anonymize messages can be longer than the values they replace,
			// so check the output size, and filter with max size if it is too large.
			// The metrics then include both passes.
			int start = output.size();
			if(!super.process(bytes, offset, length, output, metrics)) {
				return false;
			}
			if(!mustConstrainMaxSize(output.size() - start)) {
				return true;
			}
			output.setSize(start);
		}
	
		int outputSize = output.size();
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			if(process(parser, generator, () -> parser.currentLocation().getByteOffset(), () -> generator.streamWriteOutputBuffered() + output.size() - outputSize, metrics)) {
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
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		long maxSize = this.maxSize;

		boolean utf8 = JacksonMaxSizeJsonFilter.isUtf8(generator);

		int level = 0;

		PathItem pathItem = this.pathItem;

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
				pathItem.constrain(level);
				
				level--;
			case END_ARRAY:
				maxSize++;
				break;
			case VALUE_STRING:
				parser.getTextLength();
				break;
				default: // do nothing
			}

			if(nextToken == JsonToken.PROPERTY_NAME) {
				String currentName = parser.currentName();
				
				boolean prune = false;
				boolean anon = false;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level).matchPath(level, currentName);

				if(pathItem.hasType()) {
					// matched
					if(pathItem.getType() == FilterType.ANON) {
						anon = true;
					} else {
						prune = true;
					}
					pathItem = pathItem.constrain(level);
				}
				
				if(anyPathFilters != null) {
					FilterType filterType = anyPathFilters.matchPath(parser.currentName());
					if(filterType == FilterType.ANON) {
						anon = true;
					} else if(filterType == FilterType.PRUNE) {
						prune = true;
					}
				}

				if(prune) {
					// prune takes precedence over anonymize
					anon = false;
				}
				
				if(prune || anon) {				
					nextToken = parser.nextToken();
					if(nextToken == JsonToken.VALUE_STRING) {
						parser.getTextLength();
					} 

					// check size
					if(nextToken.isScalarValue()) {
						char[] message;
						if(anon) {
							message = anonymizeJsonValue;
						} else {
							message = pruneJsonValue;
						}
						
						long size = JacksonMaxSizeJsonFilter.getSeparatorSize(parser, nextToken) + JacksonMaxSizeJsonFilter.getQuotedSize(currentName, utf8) + 1 + JacksonMaxSizeJsonFilter.getRawSize(message, utf8) + outputSizeSupplier.getAsLong();

						if(size > maxSize) {
							if(metrics != null) {
								metrics.onMaxSize(-1);
							}
							break;
						}
						
						generator.writeName(currentName);
						generator.writeRawValue(message, 0, message.length);
					} else {
						// array or object
						if(anon) {
							
							// field name, colon, and opening and closing bracket
							long size = JacksonMaxSizeJsonFilter.getSeparatorSize(parser, nextToken) + JacksonMaxSizeJsonFilter.getQuotedSize(currentName, utf8) + 3 + outputSizeSupplier.getAsLong();
							
							if(size > maxSize) {
								if(metrics != null) {
									metrics.onMaxSize(-1);
								}
								break;
							}
							
							generator.writeName(currentName);
							JacksonEvents.copyCurrentEvent(parser, generator);
							// keep structure, but mark all values
							if(!anonymizeChildren(parser, generator, maxSize - 1, outputSizeSupplier, anonymizeJsonValue, metrics)) {
								break;
							}
						} else {
							long size = JacksonMaxSizeJsonFilter.getSeparatorSize(parser, nextToken) + JacksonMaxSizeJsonFilter.getQuotedSize(currentName, utf8) + 1 + JacksonMaxSizeJsonFilter.getRawSize(pruneJsonValue, utf8) + outputSizeSupplier.getAsLong();
							
							if(size > maxSize) {
								if(metrics != null) {
									metrics.onMaxSize(-1);
								}

								break;
							}
							generator.writeName(currentName);
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							
							parser.skipChildren(); // skip children
							
							if(metrics != null) {
								metrics.onPrune(1);
							}
						}

					}
					offset = offsetSupplier.getAsLong();
				} else {
					fieldName = currentName;
				}
				
				continue;
			} 
			
			long nextOffset = offsetSupplier.getAsLong();
			
			long size = nextOffset - offset; // i.e. this includes whitespace

			long outputSize = outputSizeSupplier.getAsLong();

			if(outputSize + size > maxSize) {

				// do more accurate size calculation
				int accurateSize = JacksonMaxSizeMaxStringLengthJsonFilter.getAccurateMaxStringSize(parser, fieldName, nextToken, maxStringLength, truncateStringValue, utf8);

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
		
		return true;
	}
	

	protected static boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, long maxSize, LongSupplier outputSizeSupplier, char[] anonymizeJsonValue, JsonFilterMetrics metrics) throws IOException {
		int level = 1;
		
		boolean utf8 = JacksonMaxSizeJsonFilter.isUtf8(generator);
		
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
			case PROPERTY_NAME: 
				fieldName = parser.currentName();
				continue;
			case VALUE_STRING:
				parser.getTextLength();
				break;
				default: // do nothing
			}

			long outputSize;
			if(nextToken.isScalarValue()) {
				outputSize = JacksonMaxSizeJsonFilter.getRawSize(anonymizeJsonValue, utf8);
			} else {
				outputSize = 1;
			}
			if(fieldName != null) {
				outputSize += JacksonMaxSizeJsonFilter.getQuotedSize(fieldName, utf8) + 1; // colon
			}
			outputSize += JacksonMaxSizeJsonFilter.getSeparatorSize(parser, nextToken);
			
			long size = outputSizeSupplier.getAsLong();
			
			if(outputSize + size > maxSize) {
				if(metrics != null) {
					metrics.onMaxSize(-1);
				}
				return false;
			}

			if(fieldName != null) {
				generator.writeName(fieldName);
				fieldName = null;
			}

			if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
				
				if(metrics != null) {
					metrics.onAnonymize(1);
				}
			} else {
				JacksonEvents.copyCurrentEvent(parser, generator);
			}
		}

		return true;
	}


}