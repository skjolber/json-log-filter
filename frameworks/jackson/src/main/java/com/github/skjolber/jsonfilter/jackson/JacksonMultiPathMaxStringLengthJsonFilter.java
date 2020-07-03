package com.github.skjolber.jsonfilter.jackson;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.CharArrayRangesFilter;

public class JacksonMultiPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, new JsonFactory());
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
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
						if(anon) {
							generator.writeString(CharArrayRangesFilter.FILTER_ANONYMIZE);
						} else {
							generator.writeString(CharArrayRangesFilter.FILTER_PRUNE_MESSAGE);
						}
					} else {
						// array or object
						if(anon) {
							generator.copyCurrentEvent(parser);

							// keep structure, but mark all values
							anonymizeChildren(parser, generator);
						} else {
							generator.writeString(CharArrayRangesFilter.FILTER_PRUNE_MESSAGE);
							parser.skipChildren(); // skip children
						}
					}
					
					if(level < elementMatches.length) {
						constrainMatches(elementMatches, level);
					}

					continue;
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