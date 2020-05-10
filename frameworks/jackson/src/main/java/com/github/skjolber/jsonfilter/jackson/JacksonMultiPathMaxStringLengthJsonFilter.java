package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayFilter;
import com.github.skjolber.jsonfilter.base.StringBuilderWriter;

public class JacksonMultiPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		super(maxStringLength, anonymizes, prunes);
		
		this.jsonFactory = jsonFactory;
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, new JsonFactory());
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		if(chars.length < offset + length) {
			return false;
		}

		try (JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output))) {
			return process(chars, offset, length, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(InputStream in, JsonGenerator generator) throws Exception {
		try (final JsonParser parser = jsonFactory.createParser(in)) {
			return process(parser, generator);
		}
	}

	public boolean process(byte[] chars, int offset, int length, JsonGenerator generator) throws Exception {
		if(chars.length < offset + length) {
			return false;
		}
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(char[] chars, int offset, int length, JsonGenerator generator) throws Exception {
		if(chars.length < offset + length) {
			return false;
		}
		try (final JsonParser parser = jsonFactory.createParser(chars, offset, length)) {
			return process(parser, generator);
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws Exception {
		final int[] elementFilterStart = this.elementFilterStart;
		final int[] elementMatches = new int[elementFilters.length];

		int level = 0;

		while(true) {
			JsonToken nextToken = parser.nextToken();
			if(nextToken == null) {
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
				if(level < elementFilterStart.length) {
					if(matchElements(parser.getCurrentName(), level, elementMatches)) {
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
				}
				
				if(anyElementFilters != null) {
					FilterType filterType = matchAnyElements(parser.getCurrentName());
					if(filterType == FilterType.ANON) {
						anon = true;
					} else {
						prune = true;
					}
				}
				
				
				if(prune || anon) {
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					if(nextToken.isScalarValue()) {
						if(anon) {
							generator.writeString(CharArrayFilter.FILTER_ANONYMIZE);
						} else {
							generator.writeString(CharArrayFilter.FILTER_PRUNE_MESSAGE);
						}
					} else {
						// array or object
						if(anon) {
							generator.copyCurrentEvent(parser);

							// keep structure, but mark all values
							anonymizeChildren(parser, generator);
						} else {
							generator.writeString(CharArrayFilter.FILTER_PRUNE_MESSAGE);
							parser.skipChildren(); // skip children
						}
					}
					
					if(level < elementMatches.length) {
						constrainMatches(elementMatches, level);
					}

					continue;
				}
			} else if(nextToken == JsonToken.VALUE_STRING) {
				if(parser.getTextLength() > maxStringLength) {
					String text = parser.getText();
					generator.writeString(text.substring(0, maxStringLength) + CharArrayFilter.FILTER_TRUNCATE_MESSAGE + (text.length() - maxStringLength));
					
					continue;
				}
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
				generator.writeString(CharArrayFilter.FILTER_ANONYMIZE);

				continue;
			}

			generator.copyCurrentEvent(parser);
		}
	}

}