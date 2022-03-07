package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractSingleStringFullPathJsonFilter;

public class JacksonSinglePathMaxStringLengthJsonFilter extends AbstractSingleStringFullPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type) {
		this(maxStringLength, expression, type, new JsonFactory());
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, JsonFactory jsonFactory) {
		this(maxStringLength, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, expression, type,  pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, expression, type, pruneMessage, anonymizeMessage, truncateMessage, -1);

		this.jsonFactory = jsonFactory;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
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
		try (final JsonParser parser = jsonFactory.createParser(bytes, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2)); // i.e
		
		final String[] elementPaths = this.paths;

		int level = 0;
		int matches = 0;
		
		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
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
				if(matches + 1 == level && matches < elementPaths.length && matchPath(parser.getCurrentName(), elementPaths[matches])) {
					matches++;
					
					if(matches == elementPaths.length) {
						generator.copyCurrentEvent(parser);

						nextToken = parser.nextToken();
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
								anonymizeChildren(parser, generator);
							} else {
								generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
								parser.skipChildren(); // skip children
							}
						}

						matches--;
						
						continue;
					}
				}
			} else if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				String text = parser.getText();
				
				// A high surrogate precedes a low surrogate.
				// check last include character

				builder.append('"');

				int max;
				if(Character.isLowSurrogate(text.charAt(maxStringLength))) {
					max = maxStringLength - 1;
				} else {
					max = maxStringLength;
				}

				quoteAsString(text.substring(0, max), builder);
				builder.append(truncateStringValue);
				builder.append(text.length() - max);
				builder.append('"');
				
				generator.writeRawValue(builder.toString());
				builder.setLength(0);
				
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

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		//output.ensureCapacity(output.length() + length);

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