package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;

/**
 * Any field 
 *
 */

public class JacksonMultiAnyPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;
	private final Map<String, FilterType> fields;
	
	public JacksonMultiAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, new JsonFactory());
	}

	public JacksonMultiAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMultiAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMultiAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		this.jsonFactory = jsonFactory;
		
		if(elementFilters.length > 0) {
			throw new IllegalArgumentException("Only any-element expression expected");
		}
		
		if(anyElementFilters == null) {
			fields = Collections.emptyMap();
		} else {
			fields = new HashMap<>(anyElementFilters.length * 4);
		}
		
		if(prunes != null) {
			for(int i = 0; i < prunes.length; i++) {
				fields.put(prunes[i].substring(2), FilterType.PRUNE);
			}
		}

		if(anonymizes != null) {
			for(int i = 0; i < anonymizes.length; i++) {
				fields.put(anonymizes[i].substring(2), FilterType.ANON);
			}
		}
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

		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
				break;
			}

			if(nextToken == JsonToken.FIELD_NAME) {
				
				FilterType filterType = fields.get(parser.getCurrentName());
				if(filterType != null) {
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
					
					continue;
				}
			} else if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
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

	@Override
	public boolean process(byte[] chars, int offset, int length, OutputStream output) {
		//output.ensureCapacity(output.length() + length);

		try (JsonGenerator generator = jsonFactory.createGenerator(output)) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

}