package com.github.skjolber.jsonfilter.test.truth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MaxSizeJsonFilterAssertionTest {

	private static final List<String> EXPECTED = MaxSizeJsonFilterAssertion.getTokens("{\"a\":[1,2],\"b\":\"*\"}");

	@Test
	public void testGetTokens() {
		assertEquals(List.of("START_OBJECT {", "PROPERTY_NAME a", "VALUE_NUMBER_INT 1", "END_OBJECT }"), MaxSizeJsonFilterAssertion.getTokens("{\"a\" : 1}"));
		assertEquals(List.of("VALUE_STRING abc"), MaxSizeJsonFilterAssertion.getTokens("\"abc\""));

		// not a single JSON value
		for(String json : new String[] {"{\"a\":1", "{\"a\":1}}", "{} {}", "1 2", "{\"a\":}", ""}) {
			assertNull(MaxSizeJsonFilterAssertion.getTokens(json), json);
			assertNull(MaxSizeJsonFilterAssertion.getTokens(json.getBytes(StandardCharsets.UTF_8)), json);
		}

		// a multi-byte character cut in half
		byte[] bytes = "[\"æ\"]".getBytes(StandardCharsets.UTF_8);
		assertNull(MaxSizeJsonFilterAssertion.getTokens(new byte[] {bytes[0], bytes[1], bytes[2], bytes[4], bytes[5]}));
	}

	@Test
	public void testGetPrefixMismatch() {
		// values left out at the end of each object or array
		for(String json : new String[] {"{}", "{\"a\":[]}", "{\"a\":[1]}", "{\"a\":[1,2]}"}) {
			assertNull(MaxSizeJsonFilterAssertion.getPrefixMismatch(MaxSizeJsonFilterAssertion.getTokens(json), EXPECTED), json);
		}

		// other values, or values left out before the end
		for(String json : new String[] {"{\"a\":[2]}", "{\"a\":[1],\"b\":\"*\"}", "{\"b\":\"*\"}", "{\"a\":[1,2],\"b\":\"secret\"}", "[]"}) {
			assertNotNull(MaxSizeJsonFilterAssertion.getPrefixMismatch(MaxSizeJsonFilterAssertion.getTokens(json), EXPECTED), json);
		}

		// duplicate keys are compared in order
		List<String> duplicates = MaxSizeJsonFilterAssertion.getTokens("{\"k\":\"*\",\"k\":\"v2\"}");
		assertNull(MaxSizeJsonFilterAssertion.getPrefixMismatch(MaxSizeJsonFilterAssertion.getTokens("{\"k\":\"*\"}"), duplicates));
		assertNotNull(MaxSizeJsonFilterAssertion.getPrefixMismatch(MaxSizeJsonFilterAssertion.getTokens("{\"k\":\"v2\"}"), duplicates));
	}

	@Test
	public void testGetMaxSizeFailure() {
		assertEquals("no output", MaxSizeJsonFilterAssertion.getMaxSizeFailure((String)null, 10, 2, EXPECTED, false));
		assertEquals("no output", MaxSizeJsonFilterAssertion.getMaxSizeFailure((byte[])null, 10, 2, EXPECTED, false));

		// empty output only when there is no room for an empty object or array
		assertNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure("", 1, 2, EXPECTED, false));
		assertNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure(new byte[0], 1, 2, EXPECTED, false));
		assertEquals("empty output", MaxSizeJsonFilterAssertion.getMaxSizeFailure(" ", 2, 2, EXPECTED, false));
		assertEquals("empty output", MaxSizeJsonFilterAssertion.getMaxSizeFailure(" ".getBytes(StandardCharsets.UTF_8), 2, 2, EXPECTED, false));

		assertEquals("output which is not a single JSON value", MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\":[1]}]}", 10, 2, EXPECTED, false));
		assertNotNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\":[1,2],\"b\":\"secret\"}", 30, 2, EXPECTED, false));
		assertNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\":[1]}", 10, 2, EXPECTED, false));
		assertNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\":[1]}".getBytes(StandardCharsets.UTF_8), 10, 2, EXPECTED, false));

		// with room for the input and the output without max size, the output must be complete
		assertEquals("other output than without max size", MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\":[1]}", 30, 2, EXPECTED, true));
		assertNull(MaxSizeJsonFilterAssertion.getMaxSizeFailure("{\"a\" : [1, 2], \"b\" : \"*\"}", 30, 2, EXPECTED, true));
	}
}
