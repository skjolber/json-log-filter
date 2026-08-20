package com.github.skjolber.jsonfilter.simdjson;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

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
		super(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);

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
		String s = new String(chars, offset, length);
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		return process(bytes, 0, bytes.length, output, metrics);
	}

	public boolean process(byte[] bytes, int offset, int length, StringBuilder output, JsonFilterMetrics metrics) {
		if (bytes.length < offset + length) {
			return false;
		}
		try {
			SimdJsonParser parser = getParser();
			byte[] input = SimdJsonMaxStringLengthJsonFilter.toInputArray(bytes, offset, length);
			JsonValue root = parser.parse(input, input.length);
			output.ensureCapacity(output.length() + length);
			processValue(root, output, metrics);
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

	private void processValue(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
		if (value.isString()) {
			writeStringWithMaxLength(value.asString(), output, metrics);
		} else if (value.isObject()) {
			processObject(value, output, metrics);
		} else if (value.isArray()) {
			processArray(value, output, metrics);
		} else {
			writeValue(value, output);
		}
	}

	private void processObject(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
		output.append('{');
		Iterator<Map.Entry<String, JsonValue>> it = value.objectIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			Map.Entry<String, JsonValue> entry = it.next();
			String fieldName = entry.getKey();
			JsonValue fieldValue = entry.getValue();
			writeString(fieldName, output);
			output.append(':');

			FilterType filterType = fields.get(fieldName);
			if (filterType == FilterType.ANON) {
				if (fieldValue.isObject() || fieldValue.isArray()) {
					anonymizeChildren(fieldValue, anonymizeJsonValue, output);
				} else {
					output.append(anonymizeJsonValue);
				}
				if (metrics != null) {
					metrics.onAnonymize(1);
				}
			} else if (filterType == FilterType.PRUNE) {
				output.append(pruneJsonValue);
				if (metrics != null) {
					metrics.onPrune(1);
				}
			} else {
				processValue(fieldValue, output, metrics);
			}
			first = false;
		}
		output.append('}');
	}

	private void processArray(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
		output.append('[');
		Iterator<JsonValue> it = value.arrayIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			processValue(it.next(), output, metrics);
			first = false;
		}
		output.append(']');
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
		writeString(s, output);
	}

}
