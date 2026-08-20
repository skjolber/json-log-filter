package com.github.skjolber.jsonfilter.simdjson;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractMultiPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.PathItem;

public class SimdJsonPathMaxStringLengthJsonFilter extends AbstractMultiPathJsonFilter {

	public SimdJsonPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes) {
		this(maxStringLength, anonymizes, prunes, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
	}

	public SimdJsonPathMaxStringLengthJsonFilter(int maxStringLength, String[] anonymizes, String[] prunes,
			String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, -1, -1, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if (chars.length < offset + length) {
			return false;
		}
		String s = new String(chars, offset, length);
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		return process(bytes, 0, bytes.length, output, metrics);
	}

	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if (bytes.length < offset + length) {
			return false;
		}
		try {
			SimdJsonParser parser = AbstractSimdJsonJsonFilter.getParser();
			byte[] input = SimdJsonMaxStringLengthJsonFilter.toInputArray(bytes, offset, length);
			JsonValue root = parser.parse(input, input.length);
			output.ensureCapacity(output.length() + length);
			processValue(root, output, 0, pathItem, metrics);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean process(byte[] bytes, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics metrics) {
		if (bytes.length < offset + length) {
			return false;
		}
		try {
			StringBuilder sb = new StringBuilder(length);
			if (!process(bytes, offset, length, sb, metrics)) {
				return false;
			}
			byte[] result = sb.toString().getBytes(StandardCharsets.UTF_8);
			output.write(result, 0, result.length);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected PathItem processValue(JsonValue value, StringBuilder output, int level, PathItem pathItem, JsonFilterMetrics metrics) {
		if (value.isObject()) {
			return processObject(value, output, level, pathItem, metrics);
		} else if (value.isArray()) {
			return processArray(value, output, level, pathItem, metrics);
		} else if (value.isString()) {
			writeStringWithMaxLength(value.asString(), output, metrics);
			return pathItem;
		} else {
			AbstractSimdJsonJsonFilter.writeValue(value, output);
			return pathItem;
		}
	}

	protected PathItem processObject(JsonValue value, StringBuilder output, int level, PathItem pathItem, JsonFilterMetrics metrics) {
		output.append('{');
		int objectLevel = level + 1;
		Iterator<Map.Entry<String, JsonValue>> it = value.objectIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			Map.Entry<String, JsonValue> entry = it.next();
			String fieldName = entry.getKey();
			JsonValue fieldValue = entry.getValue();

			boolean prune = false;
			boolean anon = false;

			pathItem = pathItem.constrain(objectLevel);

			if (pathItem.getLevel() == objectLevel) {
				PathItem matched = pathItem.matchPath(objectLevel, fieldName);
				if (matched.hasType()) {
					if (matched.getType() == FilterType.ANON) {
						anon = true;
					} else {
						prune = true;
					}
					pathItem = matched.constrain(objectLevel);
				} else {
					pathItem = matched;
				}
			}

			if (anyPathFilters != null) {
				FilterType filterType = anyPathFilters.matchPath(fieldName);
				if (filterType == FilterType.ANON) {
					anon = true;
				} else if (filterType == FilterType.PRUNE) {
					prune = true;
				}
			}

			AbstractSimdJsonJsonFilter.writeString(fieldName, output);
			output.append(':');

			if (anon) {
				if (fieldValue.isObject() || fieldValue.isArray()) {
					AbstractSimdJsonJsonFilter.anonymizeChildren(fieldValue, anonymizeJsonValue, output);
				} else {
					output.append(anonymizeJsonValue);
				}
				if (metrics != null) {
					metrics.onAnonymize(1);
				}
			} else if (prune) {
				output.append(pruneJsonValue);
				if (metrics != null) {
					metrics.onPrune(1);
				}
			} else {
				pathItem = processValue(fieldValue, output, objectLevel, pathItem, metrics);
			}
			first = false;
		}
		// constrain pathItem back when leaving this object
		pathItem = pathItem.constrain(objectLevel);
		output.append('}');
		return pathItem;
	}

	protected PathItem processArray(JsonValue value, StringBuilder output, int level, PathItem pathItem, JsonFilterMetrics metrics) {
		output.append('[');
		Iterator<JsonValue> it = value.arrayIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			pathItem = processValue(it.next(), output, level, pathItem, metrics);
			first = false;
		}
		output.append(']');
		return pathItem;
	}

	private void writeStringWithMaxLength(String s, StringBuilder output, JsonFilterMetrics metrics) {
		if (maxStringLength >= 0 && s.length() > maxStringLength) {
			int keepLength = maxStringLength;
			if (keepLength > 0 && Character.isLowSurrogate(s.charAt(keepLength))) {
				keepLength--;
			}
			int removeLength = s.length() - keepLength;
			int actualReductionLength = removeLength - truncateStringValue.length - lengthToDigits(removeLength);
			if (actualReductionLength > 0) {
				output.append('"');
				SimdJsonMaxStringLengthJsonFilter.writeEscaped(s, 0, keepLength, output);
				output.append(truncateStringValue);
				output.append(removeLength);
				output.append('"');
				if (metrics != null) {
					metrics.onMaxStringLength(1);
				}
				return;
			}
		}
		AbstractSimdJsonJsonFilter.writeString(s, output);
	}

}
