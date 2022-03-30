package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JacksonSinglePathMaxSizeMaxStringLengthJsonFilter extends JacksonSinglePathMaxStringLengthJsonFilter implements JacksonJsonFilter {

	public JacksonSinglePathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type) {
		this(maxStringLength, maxSize, expression, type, new JsonFactory());
	}

	public JacksonSinglePathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonSinglePathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, maxSize, expression, type,  pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonSinglePathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		this(maxStringLength, maxSize, -1, expression, type, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	protected JacksonSinglePathMaxSizeMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, maxPathMatches, expression, type, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(length <= maxSize) {
			return super.process(chars, offset, length, output);
		}

		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		if(length <= maxSize) {
			return super.process(bytes, offset, length, output);
		}

		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(bytes, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] bytes, int offset, int length, JsonGenerator generator) {
		if(length <= maxSize) {
			return super.process(bytes, offset, length, generator);
		}
		try (final JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) {
		if(length <= maxSize) {
			return super.process(chars, offset, length, generator);
		}
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e

		final int maxSize = this.maxSize - 5; // account for 4x double quotes and a colon

		final String[] elementPaths = this.paths;

		int level = 0;
		int matches = 0;
		
        String fieldName = null;
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			long size = parser.currentLocation().getCharOffset();
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
				if(size + fieldName.length() > maxSize) {
					break;
				}
				continue;
			} else if(nextToken == JsonToken.VALUE_STRING) {
				// preemptive size check for string value
				
				int length = Math.min(maxStringLength, parser.getTextLength());
				if(fieldName != null) {
					if(size + fieldName.length() + length > maxSize) {
						break;
					}
				} else {
					if(length > maxSize) {
						break;
					}
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
						if(filterType == FilterType.ANON) {
							generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
						}
					} else {
						// array or object
						if(filterType == FilterType.ANON) {
							generator.copyCurrentEvent(parser);

							// keep structure, but mark all values
							if(!anonymizeChildren(parser, generator, maxSize)) {
								break;
							}
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							parser.skipChildren(); // skip children
						}
					}

					matches--;
					
					continue;
				}
			}
			
			if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
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
	
	
	public boolean anonymizeChildren(final JsonParser parser, JsonGenerator generator, int maxSize) throws IOException {
		int level = 1;
		
        String fieldName = null;
		while(level > 0) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}
			
			long size = parser.currentLocation().getCharOffset();
			if(size >= maxSize) {
				return false;
			}
			
			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken == JsonToken.FIELD_NAME) {
				fieldName = parser.currentName();
				if(size + fieldName.length() > maxSize) {
					return false;
				}
				continue;
			}

			if(nextToken.isScalarValue()) {
				if(size + fieldName.length() + anonymizeJsonValue.length > maxSize) {
					return false;
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

		return true;
	}
	
	@Override
	public boolean process(byte[] chars, int offset, int length, OutputStream output) {
		if(length <= maxSize) {
			return super.process(chars, offset, length, output);
		}

		try (JsonGenerator generator = jsonFactory.createGenerator(output)) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
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