package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractSingleStringFullPathJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class JacksonSinglePathMaxStringLengthJsonFilter extends AbstractSingleStringFullPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type) {
		this(maxStringLength, expression, type, new JsonFactory());
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, JsonFactory jsonFactory) {
		this(maxStringLength, expression, type, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, expression, type,  pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonSinglePathMaxStringLengthJsonFilter(int maxStringLength, String expression, FilterType type, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, expression, type, pruneMessage, anonymizeMessage, truncateMessage);

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

	public boolean process(InputStream in, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] chars, int offset, int length, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) throws IOException {
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
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
								generator.writeString(CharArrayRangesFilter.FILTER_ANONYMIZE);
							} else {
								generator.writeString(CharArrayRangesFilter.FILTER_PRUNE_MESSAGE);
							}
						} else {
							// array or object
							if(filterType == FilterType.ANON) {
								generator.copyCurrentEvent(parser);

								// keep structure, but mark all values
								anonymizeChildren(parser, generator);
							} else {

								generator.writeString(CharArrayRangesFilter.FILTER_PRUNE_MESSAGE);
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
				if(Character.isHighSurrogate(text.charAt(maxStringLength - 1))) {
					generator.writeString(text.substring(0, maxStringLength - 1) + CharArrayRangesFilter.FILTER_TRUNCATE_MESSAGE + (text.length() - maxStringLength + 1));
				} else {
					generator.writeString(text.substring(0, maxStringLength) + CharArrayRangesFilter.FILTER_TRUNCATE_MESSAGE + (text.length() - maxStringLength));
				}
				
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
				generator.writeString(CharArrayRangesFilter.FILTER_ANONYMIZE);

				continue;
			}

			generator.copyCurrentEvent(parser);
		}  				

	}

}