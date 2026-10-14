package com.github.skjolber.jsonfilter.core.pp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Empty objects are kept on one line, like empty arrays.
 */

public class PrettyPrintingEmptyObjectTest {

	private final PrettyPrintingJsonFilter filter = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

	private void assertPrettyPrinted(String expected, String input) {
		assertEquals(expected, filter.process(input));
		assertEquals(expected, new String(filter.process(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void emptyObject() {
		assertPrettyPrinted("{}", "{}");
		assertPrettyPrinted("{}", "{ \n }");
		assertPrettyPrinted("{\n  \"a\": {},\n  \"b\": []\n}", "{\"a\":{},\"b\":[]}");
		assertPrettyPrinted("[\n  {},\n  {}\n]", "[{},{}]");
	}

	@Test
	public void nonEmptyObject() {
		assertPrettyPrinted("{\n  \"a\": {\n    \"b\": 1\n  }\n}", "{\"a\":{\"b\":1}}");
	}
}
