package com.github.skjolber.jsonfilter.jackson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.path.PathItem;

public class JacksonMultiPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter implements JacksonJsonFilter {

	protected final JsonFactory jsonFactory;

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, new JsonFactory());
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, JsonFactory jsonFactory) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE, jsonFactory);
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		this(maxStringLength, -1, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);
	}
	
	protected JacksonMultiPathMaxStringLengthJsonFilter(int maxStringLength, int maxSize, int maxPathMatches,
			String[] anonymizes, String[] prunes, String pruneMessage, String anonymizeMessage,
			String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, maxSize, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		
		this.jsonFactory = jsonFactory;
	}

	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output) {
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output) {
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator) throws IOException {
		return process(parser, generator, null);
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

	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}
	
	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		output.ensureCapacity(output.length() + length);

		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, ByteArrayOutputStream output, JsonFilterMetrics metrics) {
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
			) {
			return process(parser, generator, metrics);
		} catch(final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2));

		int level = 0;

		PathItem pathItem = this.pathItem;

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
			} else if(nextToken == JsonToken.FIELD_NAME) {
				String currentName = parser.currentName();
				
				boolean prune = false;
				boolean anon = false;
				
				// match again any higher filter
				pathItem = pathItem.constrain(level);
						
				if(pathItem.getLevel() == level) {
					pathItem = pathItem.matchPath(level, currentName);
					
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
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					if(nextToken.isScalarValue()) {
						if(anon) {
							generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
						}
					} else {
						// array or object
						if(anon) {
							generator.copyCurrentEvent(parser);

							// keep structure, but mark all values
							anonymizeChildren(parser, generator, metrics);
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							parser.skipChildren(); // skip children
							
							if(metrics != null) {
								metrics.onPrune(1);
							}
						}
					}

					continue;
				}
			} else if(nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				JacksonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				
				if(metrics != null) {
					metrics.onMaxStringLength(1);
				}
				
				continue;
			}
			
			generator.copyCurrentEvent(parser);
		}

		return true;
	}	

	protected void anonymizeChildren(JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
		int level = 1;

		while(level > 0) {
			JsonToken nextToken = parser.nextToken();

			if(nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if(nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if(nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);

				if(metrics != null) {
					metrics.onAnonymize(1);
				}
				
				continue;
			}

			generator.copyCurrentEvent(parser);
		}
	}
	
}