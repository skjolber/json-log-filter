package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.DefaultJsonFilterMetrics;

public class DefaultJacksonJsonFilterTest {

	@Test
	public void testException() throws Exception {
		
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();
		
		assertNull(filter.process(new byte[] {}, 0, 100));
		assertFalse(filter.process(new char[] {}, 0, 100, new StringBuilder()));
		
		String illegalJson = "{abcdef}";
		assertFalse(filter.process(illegalJson.toCharArray(), 0, 3, new StringBuilder()));
		assertFalse(filter.process(illegalJson.getBytes(StandardCharsets.UTF_8), 0, 3, new ResizableByteArrayOutputStream(1024)));
	}
	
	@Test
	public void testExceptionConstructor() throws Exception {
		
		JsonFactory factory = new JsonFactory();
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter(factory);
		
		assertNull(filter.process(new byte[] {}, 0, 100));
		assertFalse(filter.process(new char[] {}, 0, 100, new StringBuilder()));
	}

	@Test
	public void testProcessByteArrayToStringBuilder() throws Exception {
		// Verify process(byte[], int, int, StringBuilder, metrics) works for valid and invalid JSON
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();

		// Valid JSON: bytes should be converted to string in the StringBuilder
		String json = "{\"key\":\"value\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, sb, null));
		assertEquals("{\"key\":\"value\"}", sb.toString());

		// Valid JSON with metrics
		sb.setLength(0);
		assertTrue(filter.process(bytes, 0, bytes.length, sb, new DefaultJsonFilterMetrics()));
		assertEquals("{\"key\":\"value\"}", sb.toString());

		// Invalid JSON returns false (via parse() returning false, not exception)
		byte[] invalid = "{invalid".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(invalid, 0, invalid.length, new StringBuilder(), null));
	}

	@Test
	public void testSizeMetrics() throws Exception {
		// input and output sizes, also when the output already holds content
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();

		String json = "{\"a\":\"secret\",\"b\":1,\"c\":{\"d\":true},\"e\":[1,2]}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		StringBuilder builder = new StringBuilder("[");
		assertTrue(filter.process(json.toCharArray(), 0, json.length(), builder, metrics));
		assertEquals(json.length(), metrics.getInputSize());
		assertEquals(builder.length() - 1, metrics.getOutputSize());

		metrics = new DefaultJsonFilterMetrics();
		builder = new StringBuilder("[");
		assertTrue(filter.process(bytes, 0, bytes.length, builder, metrics));
		assertEquals(bytes.length, metrics.getInputSize());
		assertEquals(builder.length() - 1, metrics.getOutputSize());

		metrics = new DefaultJsonFilterMetrics();
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		output.write('[');
		assertTrue(filter.process(bytes, 0, bytes.length, output, metrics));
		assertEquals(bytes.length, metrics.getInputSize());
		assertEquals(output.size() - 1, metrics.getOutputSize());
	}

	@Test
	public void testProcessByteArrayToStringBuilderExceptionFromFactory() throws Exception {
		// Cover catch block in process(byte[], StringBuilder): factory.createParser throws
		JsonFactory jsonFactory = mock(JsonFactory.class);
		when(jsonFactory.createParser(any(byte[].class), anyInt(), anyInt())).thenThrow(new RuntimeException("test"));
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter(jsonFactory);
		byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(json, 0, json.length, new StringBuilder(), null));
	}

	@Test
	public void testParseRejectsMultipleRootValues() throws Exception {
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();
		JsonFactory jsonFactory = new JsonFactory();

		String json = IOUtils.toString(getClass().getResourceAsStream("/rootValues/multiple/objects.json"), StandardCharsets.UTF_8);
		char[] chars = json.toCharArray();
		try (JsonParser parser = jsonFactory.createParser(chars, 0, chars.length)) {
			assertFalse(filter.parse(parser));
		}
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		try (JsonParser parser = jsonFactory.createParser(bytes, 0, bytes.length)) {
			assertFalse(filter.parse(parser));
		}

		String single = IOUtils.toString(getClass().getResourceAsStream("/rootValues/single/objectKey.json"), StandardCharsets.UTF_8);
		chars = single.toCharArray();
		try (JsonParser parser = jsonFactory.createParser(chars, 0, chars.length)) {
			assertTrue(filter.parse(parser));
		}
		bytes = single.getBytes(StandardCharsets.UTF_8);
		try (JsonParser parser = jsonFactory.createParser(bytes, 0, bytes.length)) {
			assertTrue(filter.parse(parser));
		}
	}
}
