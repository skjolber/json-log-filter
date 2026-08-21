package com.github.skjolber.jsonfilter.simdjson;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.StringBuilderWriter;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class SimdJsonAnyPathMaxStringLengthJsonFilter extends AbstractSimdJsonJsonFilter {

	private final Map<String, FilterType> fields;

	public SimdJsonAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public SimdJsonAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage) {
		this(maxStringLength, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage, new JsonFactory());
	}

	public SimdJsonAnyPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage, JsonFactory jsonFactory) {
		super(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage, jsonFactory);

		Map<String, FilterType> map = new HashMap<>(64);
		if (prunes != null) {
			for (String prune : prunes) {
				if (!AbstractMultiPathJsonFilter.hasAnyPrefix(prune)) {
					throw new IllegalArgumentException("Only any-element expression expected: " + prune);
				}
				map.put(AbstractMultiPathJsonFilter.removeAnyPrefix(prune), FilterType.PRUNE);
			}
		}
		if (anonymizes != null) {
			for (String anonymize : anonymizes) {
				if (!AbstractMultiPathJsonFilter.hasAnyPrefix(anonymize)) {
					throw new IllegalArgumentException("Only any-element expression expected: " + anonymize);
				}
				map.put(AbstractMultiPathJsonFilter.removeAnyPrefix(anonymize), FilterType.ANON);
			}
		}
		this.fields = Collections.unmodifiableMap(map);
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

		while (true) {
			JsonToken nextToken = parser.nextToken();
			if (nextToken == null) {
				break;
			}

			if (nextToken == JsonToken.PROPERTY_NAME) {
				FilterType filterType = fields.get(parser.currentName());
				if (filterType != null) {
					generator.copyCurrentEvent(parser);

					nextToken = parser.nextToken();
					if (nextToken.isScalarValue()) {
						if (filterType == FilterType.ANON) {
							generator.writeRawValue(anonymizeJsonValue, 0, anonymizeJsonValue.length);
							if (metrics != null) {
								metrics.onAnonymize(1);
							}
						} else {
							generator.writeRawValue(pruneJsonValue, 0, pruneJsonValue.length);
							if (metrics != null) {
								metrics.onPrune(1);
							}
						}
					} else {
						// array or object value
						if (filterType == FilterType.ANON) {
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
		generator.flush();

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
