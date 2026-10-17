package com.github.skjolber.jsonfilter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ByteArrayWhitespaceFilterTest {

	@Test
	public void testSkipWhitespace() {
		// the skip checks four characters per iteration, so cover every run length modulo four,
		// with each kind of whitespace and different characters after the run
		String[] runs = new String[] {" ", "\n", "\t", "\r", "  ", "\n ", " \t ", "\r\n  ", "\n    ", "\n     ", " \t\r\n \t\r\n", "\n         "};
		String[] prefixes = new String[] {"", "x", "{\"a\":1,", "[1,2"};
		// not non-ASCII: like the loops it replaces, the skip takes negative bytes for whitespace, and valid JSON
		// has none outside strings
		String[] suffixes = new String[] {"\"", "{", "}", "]", "1", "t", "!"};

		for (String prefix : prefixes) {
			for (String run : runs) {
				for (String suffix : suffixes) {
					String json = prefix + run + suffix + " ";

					assertEquals(prefix.length() + run.length(), ByteArrayWhitespaceFilter.skipWhitespace(json.getBytes(StandardCharsets.UTF_8), prefix.length()), json);
				}
			}
		}
	}

}
