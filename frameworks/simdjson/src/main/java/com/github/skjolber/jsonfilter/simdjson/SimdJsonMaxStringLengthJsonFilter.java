package com.github.skjolber.jsonfilter.simdjson;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class SimdJsonMaxStringLengthJsonFilter extends AbstractSimdJsonJsonFilter {

	public SimdJsonMaxStringLengthJsonFilter(int maxStringLength) {
		this(maxStringLength, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
	}

	public SimdJsonMaxStringLengthJsonFilter(int maxStringLength, String pruneMessage, String anonymizeMessage, String truncateMessage) {
		super(maxStringLength, -1, pruneMessage, anonymizeMessage, truncateMessage);
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
			byte[] input = toInputArray(bytes, offset, length);
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

	protected void processValue(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
		if (value.isString()) {
			writeStringValue(value.asString(), output, metrics);
		} else if (value.isObject()) {
			processObject(value, output, metrics);
		} else if (value.isArray()) {
			processArray(value, output, metrics);
		} else {
			writeValue(value, output);
		}
	}

	protected void writeStringValue(String s, StringBuilder output, JsonFilterMetrics metrics) {
		if (maxStringLength >= 0 && s.length() > maxStringLength) {
			int keepLength = maxStringLength;
			if (keepLength > 0 && Character.isLowSurrogate(s.charAt(keepLength))) {
				keepLength--;
			}
			int removeLength = s.length() - keepLength;
			int actualReductionLength = removeLength - truncateStringValue.length - lengthToDigits(removeLength);
			if (actualReductionLength > 0) {
				output.append('"');
				writeEscaped(s, 0, keepLength, output);
				output.append(truncateStringValue);
				output.append(removeLength);
				output.append('"');
				if (metrics != null) {
					metrics.onMaxStringLength(1);
				}
			} else {
				writeString(s, output);
			}
		} else {
			writeString(s, output);
		}
	}

	protected void processObject(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
		output.append('{');
		Iterator<Map.Entry<String, JsonValue>> it = value.objectIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			Map.Entry<String, JsonValue> entry = it.next();
			writeString(entry.getKey(), output);
			output.append(':');
			processValue(entry.getValue(), output, metrics);
			first = false;
		}
		output.append('}');
	}

	protected void processArray(JsonValue value, StringBuilder output, JsonFilterMetrics metrics) {
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

	protected static byte[] toInputArray(byte[] bytes, int offset, int length) {
		if (offset == 0 && length == bytes.length) {
			return bytes;
		}
		byte[] copy = new byte[length];
		System.arraycopy(bytes, offset, copy, 0, length);
		return copy;
	}

	protected static void writeEscaped(String s, int start, int end, StringBuilder output) {
		for (int i = start; i < end; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"':  output.append('\\'); output.append('"');  break;
				case '\\': output.append('\\'); output.append('\\'); break;
				case '\b': output.append('\\'); output.append('b');  break;
				case '\f': output.append('\\'); output.append('f');  break;
				case '\n': output.append('\\'); output.append('n');  break;
				case '\r': output.append('\\'); output.append('r');  break;
				case '\t': output.append('\\'); output.append('t');  break;
				default:
					if (c < 0x20) {
						output.append(String.format("\\u%04x", (int) c));
					} else {
						output.append(c);
					}
			}
		}
	}

}
