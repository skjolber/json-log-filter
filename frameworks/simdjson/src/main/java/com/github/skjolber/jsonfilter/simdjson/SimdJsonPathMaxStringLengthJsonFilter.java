package com.github.skjolber.jsonfilter.simdjson;

import java.io.IOException;

import org.apache.commons.io.output.StringBuilderWriter;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.PathItem;

public class SimdJsonPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter {

	protected final JsonFactory jsonFactory;

	public SimdJsonPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public SimdJsonPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public SimdJsonPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		this.jsonFactory = jsonFactory;
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if (chars.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(chars, offset, length)
		) {
			return process(parser, generator, metrics);
		} catch (final Exception e) {
			return false;
		}
	}

	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if (bytes.length < offset + length) {
			return false;
		}
		output.ensureCapacity(output.length() + length);
		try (
			JsonGenerator generator = jsonFactory.createGenerator(new StringBuilderWriter(output));
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
		) {
			return process(parser, generator, metrics);
		} catch (final Exception e) {
			return false;
		}
	}

	@Override
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if (bytes.length < offset + length) {
			return false;
		}
		try (
			JsonGenerator generator = jsonFactory.createGenerator(output);
			JsonParser parser = jsonFactory.createParser(bytes, offset, length)
		) {
			return process(parser, generator, metrics);
		} catch (final Exception e) {
			return false;
		}
	}

	public boolean process(final JsonParser parser, JsonGenerator generator, JsonFilterMetrics metrics) throws IOException {
		StringBuilder builder = new StringBuilder(Math.max(16 * 1024, maxStringLength + 11 + truncateStringValue.length + 2));

		int level = 0;
		PathItem pathItem = this.pathItem;

		while (true) {
			JsonToken nextToken = parser.nextToken();
			if (nextToken == null) {
				break;
			}

			if (nextToken == JsonToken.START_OBJECT) {
				level++;
			} else if (nextToken == JsonToken.END_OBJECT) {
				pathItem.constrain(level);
				level--;
			} else if (nextToken == JsonToken.PROPERTY_NAME) {
				String currentName = parser.currentName();

				boolean prune = false;
				boolean anon = false;

				pathItem = pathItem.constrain(level);

				if (pathItem.getLevel() == level) {
					pathItem = pathItem.matchPath(level, currentName);

					if (pathItem.hasType()) {
						if (pathItem.getType() == FilterType.ANON) {
							anon = true;
						} else {
							prune = true;
						}
						pathItem = pathItem.constrain(level);
					}
				}

				if (anyPathFilters != null) {
					FilterType filterType = anyPathFilters.matchPath(currentName);
					if (filterType == FilterType.ANON) {
						anon = true;
					} else if (filterType == FilterType.PRUNE) {
						prune = true;
					}
				}

				if (prune || anon) {
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					if (nextToken.isScalarValue()) {
						if (anon) {
							generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
						}
					} else {
						// array or object value
						if (anon) {
							generator.copyCurrentEvent(parser);
							anonymizeChildren(parser, generator, metrics);
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							parser.skipChildren();
							if (metrics != null) {
								metrics.onPrune(1);
							}
						}
					}
					continue;
				}
			} else if (nextToken == JsonToken.VALUE_STRING && parser.getTextLength() > maxStringLength) {
				SimdJsonMaxStringLengthJsonFilter.writeMaxStringLength(parser, generator, builder, maxStringLength, truncateStringValue);
				if (metrics != null) {
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

		while (level > 0) {
			JsonToken nextToken = parser.nextToken();

			if (nextToken == JsonToken.START_OBJECT || nextToken == JsonToken.START_ARRAY) {
				level++;
			} else if (nextToken == JsonToken.END_OBJECT || nextToken == JsonToken.END_ARRAY) {
				level--;
			} else if (nextToken.isScalarValue()) {
				generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
				if (metrics != null) {
					metrics.onAnonymize(1);
				}
				continue;
			}

			generator.copyCurrentEvent(parser);
		}
	}

}
