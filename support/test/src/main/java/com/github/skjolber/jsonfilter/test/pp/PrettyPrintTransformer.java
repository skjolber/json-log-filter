package com.github.skjolber.jsonfilter.test.pp;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class PrettyPrintTransformer implements Function<String, String> {

	public static final List<PrettyPrintTransformer> ALL;
	
	static {
		ALL = PrettyPrinterIterator.getAll().stream().map(p -> new PrettyPrintTransformer(p)).collect(Collectors.toList());
	}
	
	protected final JsonFactory jsonFactory;
	protected final DefaultPrettyPrinter prettyPrinter;

	public PrettyPrintTransformer(DefaultPrettyPrinter prettyPrinter) {
		this(new JsonFactory(), prettyPrinter);
	}

	public PrettyPrintTransformer(JsonFactory jsonFactory, DefaultPrettyPrinter prettyPrinter) {
		this.jsonFactory = jsonFactory;
		this.prettyPrinter = prettyPrinter;
	}
	
	@Override
	public String apply(String t) {
		
		try (
			StringReader reader = new StringReader(t);
			CharArrayWriter writer = new CharArrayWriter(t.length() * 2);

			JsonGenerator generator = jsonFactory.createGenerator(writer).setPrettyPrinter(prettyPrinter.createInstance());
			JsonParser parser = jsonFactory.createParser(reader)
			) {
			
			while(true) {
				JsonToken nextToken = parser.nextToken();
				if(nextToken == null) {
					break;
				}
				generator.copyCurrentEvent(parser);
			}
			
			generator.flush(); // don't close
			
			String string = writer.toString();

			if(t.contains("\\u")) {
				StringBuilder builder = new StringBuilder();
				escape(string, builder);
				
				return builder.toString();
			}
			
			
			return string;
		} catch(final Exception e) {
			return t;
		}
	}

	public DefaultPrettyPrinter getPrettyPrinter() {
		return prettyPrinter;
	}
	
	/**
	 * Escape special chars form String except /
	 * 
	 * @param s
	 *            - Must not be null.
	 * @param out
	 */
	public static void escape(String s, Appendable out) {
		try {
			int len = s.length();
			for (int i = 0; i < len; i++) {
				char ch = s.charAt(i);
				if (ch >= 128) {
					out.append("\\u");
					String hex = "0123456789ABCDEF";
					out.append(hex.charAt(ch >> 12 & 0x000F));
					out.append(hex.charAt(ch >> 8 & 0x000F));
					out.append(hex.charAt(ch >> 4 & 0x000F));
					out.append(hex.charAt(ch >> 0 & 0x000F));
				} else {
					out.append(ch);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Impossible Exception");
		}
	}
}
