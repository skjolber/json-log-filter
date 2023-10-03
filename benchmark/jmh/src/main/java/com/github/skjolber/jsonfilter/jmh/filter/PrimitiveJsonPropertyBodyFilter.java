package com.github.skjolber.jsonfilter.jmh.filter;

import static java.util.regex.Pattern.compile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.skjolber.jsonfilter.JsonFilter;
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public final class PrimitiveJsonPropertyBodyFilter implements JsonFilter {

	/*language=RegExp*/
	private static final String BOOLEAN_PATTERN = "(?:true|false)";

	/*language=RegExp*/
	private static final String NUMBER_PATTERN =
			"(?:-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)";

	private static final Pattern NUMBER = pattern(NUMBER_PATTERN);

	/**
	 * @see <a href="https://stackoverflow.com/a/43597014/232539">Regex for quoted string with escaping quotes</a>
	 */
	/*language=RegExp*/
	private static final String STRING_PATTERN = "(?:\"(.*?[^\\\\])??((\\\\\\\\)+)?+\")";

	private static final Pattern STRING = pattern(STRING_PATTERN);

	/*language=RegExp*/
	private static final String PRIMITIVE_PATTERN =
			"(?:" + BOOLEAN_PATTERN + "|" + NUMBER_PATTERN + "|" + STRING_PATTERN + ")";

	private static final Pattern PRIMITIVE = pattern(PRIMITIVE_PATTERN);

	private final Pattern pattern;

	private final Predicate<String> predicate;

	private final String replacement;

	public PrimitiveJsonPropertyBodyFilter(Pattern pattern, Predicate<String> predicate, String replacement) {
		super();
		this.pattern = pattern;
		this.predicate = predicate;
		this.replacement = replacement;
	}

	private static Pattern pattern(final String value) {
		return compile("(?<key>\"(?<property>.*?)\"\\s*:\\s*)(" + value + "|null)");
	}

	public static PrimitiveJsonPropertyBodyFilter replaceString(
			final Predicate<String> predicate, final String replacement) {
		return create(STRING, predicate, quote(replacement));
	}

	private static PrimitiveJsonPropertyBodyFilter create(Pattern pattern, Predicate<String> predicate, String replacement) {
		return new PrimitiveJsonPropertyBodyFilter(pattern, predicate, replacement);
	}

	public static PrimitiveJsonPropertyBodyFilter replaceNumber(
			final Predicate<String> predicate, final Number replacement) {
		return create(NUMBER, predicate, String.valueOf(replacement));
	}

	public static PrimitiveJsonPropertyBodyFilter replacePrimitive(
			final Predicate<String> predicate, final String replacement) {
		return create(PRIMITIVE, predicate, quote(replacement));
	}

	public static String quote(final String s) {
		return "\"" + s + "\"";
	}

	public String filter(final String body) {
		final Matcher matcher = pattern.matcher(body);
		final StringBuffer result = new StringBuffer(body.length());

		while (matcher.find()) {
			if (predicate.test(matcher.group("property"))) {
				// this preserves whitespaces around properties
				matcher.appendReplacement(result, "${key}");
				result.append(replacement);
			} else {
				matcher.appendReplacement(result, "$0");
			}
		}
		matcher.appendTail(result);

		return result.toString();
	}

	@Override
	public String process(char[] chars) {
		return process(new String(chars));
	}

	@Override
	public String process(String chars) {
		return filter(chars);
	}

	@Override
	public boolean process(String chars, StringBuilder output) {
		output.append(filter(chars));
		return true;
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output) {
		output.append(filter(new String(chars, offset, length)));
		return true;
	}

	@Override
	public byte[] process(byte[] chars) {
		return process(new String(chars)).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length) {
		String process = process(new String(chars, offset, length));
		
		return process.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
		byte[] process = process(chars, offset, length);
		if(process != null) {
			output.write(process, 0, process.length);
			return true;
		}
		return false;
	}

	@Override
	public String process(char[] chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public String process(String chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public boolean process(String chars, StringBuilder output, JsonFilterMetrics filterMetrics) {
		return process(chars, output);
	}

	@Override
	public boolean process(char[] chars, int offset, int length, StringBuilder output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

	@Override
	public byte[] process(byte[] chars, JsonFilterMetrics filterMetrics) {
		return process(chars);
	}

	@Override
	public byte[] process(byte[] chars, int offset, int length, JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
			JsonFilterMetrics filterMetrics) {
		return process(chars, offset, length, output);
	}

}