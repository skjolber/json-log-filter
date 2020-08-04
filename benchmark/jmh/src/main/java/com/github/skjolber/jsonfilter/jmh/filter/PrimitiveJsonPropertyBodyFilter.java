package com.github.skjolber.jsonfilter.jmh.filter;

import static java.util.regex.Pattern.compile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.skjolber.jsonfilter.JsonFilter;

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
	public boolean process(Reader reader, int length, StringBuilder output) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean process(Reader reader, StringBuilder output) throws IOException {
		return process(reader, -1, output);
	}

	@Override
	public byte[] process(byte[] chars) {
		return process(new String(chars)).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
		String process = process(new String(chars, offset, length));
		
		byte[] bytes = process.getBytes(StandardCharsets.UTF_8);
		output.write(bytes, 0, bytes.length);
		return true;
	}

	@Override
	public boolean process(InputStream input, int length, ByteArrayOutputStream output) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean process(InputStream input, ByteArrayOutputStream output) throws IOException {
		return process(input, -1, output);
	}

	@Override
	public boolean process(byte[] chars, ByteArrayOutputStream output) {
		return process(chars, 0, chars.length, output);
	}


}