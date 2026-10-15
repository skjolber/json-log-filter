package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;

public class JacksonEventsTest {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	@Test
	public void testSingleRootValue() throws Exception {
		for(String json : readDirectory("/rootValues/single")) {
			assertTrue(isSingleRootValue(createCharArrayParser(json), Integer.MAX_VALUE), json);
			assertTrue(isSingleRootValue(createByteArrayParser(json), Integer.MAX_VALUE), json);
		}
	}

	@Test
	public void testMultipleRootValues() throws Exception {
		for(String json : readDirectory("/rootValues/multiple")) {
			assertFalse(isSingleRootValue(createCharArrayParser(json), Integer.MAX_VALUE), json);
			assertFalse(isSingleRootValue(createByteArrayParser(json), Integer.MAX_VALUE), json);
		}
	}

	@Test
	public void testStoppedWithinRootValue() throws Exception {
		// i.e. at max size: the parser stops within a nested value
		String json = IOUtils.toString(getClass().getResourceAsStream("/rootValues/multiple/objectsNested.json"), StandardCharsets.UTF_8);

		// within the array of the first root value: {, a, {, b, [
		assertTrue(isSingleRootValue(createCharArrayParser(json), 5));
		assertTrue(isSingleRootValue(createByteArrayParser(json), 5));

		// within the array of the second root value: ..., }, {, c, {, d, [
		assertFalse(isSingleRootValue(createCharArrayParser(json), 15));
		assertFalse(isSingleRootValue(createByteArrayParser(json), 15));
	}

	private static List<String> readDirectory(String directory) throws Exception {
		List<String> result = new ArrayList<>();
		try (Stream<Path> files = Files.list(Paths.get(JacksonEventsTest.class.getResource(directory).toURI()))) {
			for(Path file : files.sorted().toList()) {
				result.add(Files.readString(file, StandardCharsets.UTF_8));
			}
		}
		assertFalse(result.isEmpty(), directory);
		return result;
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
