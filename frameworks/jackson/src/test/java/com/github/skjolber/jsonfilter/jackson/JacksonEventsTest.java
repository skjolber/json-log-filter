package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;

public class JacksonEventsTest {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	@Test
	public void testSingleRootValue() {
		for(String json : new String[] {"{\"b\":1}", "{\"b\":1} \n", "[1,2]", "1", "\"x\""}) {
			assertTrue(isSingleRootValue(createCharArrayParser(json), Integer.MAX_VALUE), json);
			assertTrue(isSingleRootValue(createByteArrayParser(json), Integer.MAX_VALUE), json);
		}
	}

	@Test
	public void testMultipleRootValues() {
		String[] inputs = new String[] {
			"{\"a\":1}{\"injected\":true}",
			"{\"a\":1} {\"injected\":true}",
			"{\"a\":1}\n{\"injected\":true}\n",
			"{\"a\":1}123456789",
			"[1][2]",
			"1 2 3",
			"\"x\" \"y\"",
			"null true"
		};
		for(String json : inputs) {
			assertFalse(isSingleRootValue(createCharArrayParser(json), Integer.MAX_VALUE), json);
			assertFalse(isSingleRootValue(createByteArrayParser(json), Integer.MAX_VALUE), json);
		}
	}

	@Test
	public void testStoppedWithinRootValue() {
		// i.e. at max size: the parser stops within a nested value
		String json = "{\"a\":{\"b\":[1,2]}}{\"c\":{\"d\":[3,4]}}";

		// within the array of the first root value: {, a, {, b, [
		assertTrue(isSingleRootValue(createCharArrayParser(json), 5));
		assertTrue(isSingleRootValue(createByteArrayParser(json), 5));

		// within the array of the second root value: ..., }, {, c, {, d, [
		assertFalse(isSingleRootValue(createCharArrayParser(json), 15));
		assertFalse(isSingleRootValue(createByteArrayParser(json), 15));
	}

	private static boolean isSingleRootValue(JsonParser parser, int maxTokens) {
		try (parser) {
			for(int i = 0; i < maxTokens; i++) {
				if(parser.nextToken() == null) {
					break;
				}
			}
			return JacksonEvents.isSingleRootValue(parser);
		}
	}

	private static JsonParser createCharArrayParser(String json) {
		char[] chars = json.toCharArray();
		return JSON_FACTORY.createParser(chars, 0, chars.length);
	}

	private static JsonParser createByteArrayParser(String json) {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		return JSON_FACTORY.createParser(bytes, 0, bytes.length);
	}
}
