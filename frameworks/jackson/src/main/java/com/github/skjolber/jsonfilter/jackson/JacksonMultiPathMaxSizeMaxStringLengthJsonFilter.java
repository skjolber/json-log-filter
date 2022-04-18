package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.LongSupplier;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonMultiPathMaxSizeMaxStringLengthJsonFilter extends JacksonMultiPathMaxStringLengthJsonFilter implements JacksonJsonFilter {

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes) {
		this(maxStringLength, maxSize, anonymizes, prunes, new JsonFactory());
	}

	public JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}
	
	protected JacksonMultiPathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
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
		// measure size only based on input source offset 
		// the implementation in inaccurate to the size of an number field + max level of depth.

		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		int maxSize = this.maxSize;

		final int[] elementFilterStart = this.elementFilterStart;
		final int[] elementMatches = new int[elementFilters.length];

		int level = 0;
		
		while(true) {
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
		
					if(level < elementFilterStart.length) {
						constrainMatches(elementMatches, level);
					}
				} else if(nextToken == JsonToken.FIELD_NAME) {
					boolean prune = false;
					boolean anon = false;
					
					// match again any higher filter
					if(level < elementFilterStart.length && matchElements(parser.getCurrentName(), level, elementMatches)) {
						for(int i = elementFilterStart[level]; i < elementFilterEnd[level]; i++) {
							if(elementMatches[i] == level) {
								// matched
								if(elementFilters[i].filterType == FilterType.ANON) {
									anon = true;
								} else {
									prune = true;
									
									break;
								}
							}
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
						generator.copyCurrentEvent(parser);
						
						nextToken = parser.nextToken();
						if(nextToken.isScalarValue()) {
							char[] message;
							if(anon) {
								message = anonymizeJsonValue;
							} else {
								message = pruneJsonValue;
							}
							generator.writeRawValue(message, 0, message.length);
						} else {
							// array or object
							if(anon) {
								generator.copyCurrentEvent(parser);
		
								// keep structure, but mark all values
								maxSize = JacksonSingleFullPathMaxSizeMaxStringLengthJsonFilter.anonymizeChildren(parser, generator, maxSize, offsetSupplier, outputSizeSupplier, anonymizeJsonValue);
								if(maxSize == -1) {
									return true;
								}
							} else {
								generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
								
								parser.skipChildren(); // skip children
								
								maxSize += offsetSupplier.getAsLong() - size - pruneJsonValue.length;
							}
						}
						
						if(level < elementMatches.length) {
							constrainMatches(elementMatches, level);
						}
		
						continue;
					}
				} else if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
					// preemptive size check for string value
					JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
					
					maxSize += parser.getTextLength() - maxStringLength - truncateStringValue.length;
		
					continue;
				}
		
				generator.copyCurrentEvent(parser);
			}
			generator.flush(); // don't close
			
			// current size has been estimated, now measure it accurately

			long outputSize = outputSizeSupplier.getAsLong();
			if(outputSize < maxSize) {
				
			}
		}
		
		return true;
	}


}