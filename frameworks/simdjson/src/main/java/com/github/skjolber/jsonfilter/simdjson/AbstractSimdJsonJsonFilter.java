package com.github.skjolber.jsonfilter.simdjson;

import java.util.Iterator;
import java.util.Map;

import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

import com.github.skjolber.jsonfilter.base.AbstractJsonFilter;

public abstract class AbstractSimdJsonJsonFilter extends AbstractJsonFilter {

	protected static final ThreadLocal<SimdJsonParser> PARSER = ThreadLocal.withInitial(SimdJsonParser::new);

	protected AbstractSimdJsonJsonFilter(int maxStringLength, int maxSize, String pruneJson, String anonymizeJson, String truncateJsonString) {
		super(maxStringLength, maxSize, pruneJson, anonymizeJson, truncateJsonString);
	}

	protected static SimdJsonParser getParser() {
		return PARSER.get();
	}

	protected static void writeValue(JsonValue value, StringBuilder output) {
		if (value.isString()) {
			writeString(value.asString(), output);
		} else if (value.isObject()) {
			writeObject(value, output);
		} else if (value.isArray()) {
			writeArray(value, output);
		} else if (value.isLong()) {
			output.append(value.asLong());
		} else if (value.isDouble()) {
			output.append(value.asDouble());
		} else if (value.isBoolean()) {
			output.append(value.asBoolean());
		} else if (value.isNull()) {
			output.append("null");
		} else {
			output.append(value.toString());
		}
	}

	protected static void writeObject(JsonValue value, StringBuilder output) {
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
			writeValue(entry.getValue(), output);
			first = false;
		}
		output.append('}');
	}

	protected static void writeArray(JsonValue value, StringBuilder output) {
		output.append('[');
		Iterator<JsonValue> it = value.arrayIterator();
		boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				output.append(',');
			}
			writeValue(it.next(), output);
			first = false;
		}
		output.append(']');
	}

	protected static void writeString(String s, StringBuilder output) {
		output.append('"');
		for (int i = 0; i < s.length(); i++) {
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
		output.append('"');
	}

	protected static void writeAnonymized(JsonValue value, char[] anonymizeJsonValue, StringBuilder output) {
		if (value.isObject() || value.isArray()) {
			writeValue(value, output);
		} else {
			output.append(anonymizeJsonValue);
		}
	}

	protected static void anonymizeChildren(JsonValue value, char[] anonymizeJsonValue, StringBuilder output) {
		if (value.isObject()) {
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
				JsonValue child = entry.getValue();
				if (child.isObject() || child.isArray()) {
					anonymizeChildren(child, anonymizeJsonValue, output);
				} else {
					output.append(anonymizeJsonValue);
				}
				first = false;
			}
			output.append('}');
		} else if (value.isArray()) {
			output.append('[');
			Iterator<JsonValue> it = value.arrayIterator();
			boolean first = true;
			while (it.hasNext()) {
				if (!first) {
					output.append(',');
				}
				JsonValue child = it.next();
				if (child.isObject() || child.isArray()) {
					anonymizeChildren(child, anonymizeJsonValue, output);
				} else {
					output.append(anonymizeJsonValue);
				}
				first = false;
			}
			output.append(']');
		}
	}

}
