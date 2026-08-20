package com.github.skjolber.jsonfilter.simdjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class SimdJsonMaxStringLengthJsonFilterTest {

	@Test
	public void testPassthrough() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		String json = "{\"key\":\"value\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertNotNull(output.toString());
		assertTrue(output.toString().contains("value"));
	}

	@Test
	public void testMaxStringLength() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(3);
		String json = "{\"key\":\"longervalue\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertTrue(result.contains("lon"));
		assertFalse(result.contains("longervalue"));
	}

	@Test
	public void testInvalidJson() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		byte[] bytes = "{invalid".getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertFalse(filter.process(bytes, 0, bytes.length, output, null));
	}

	@Test
	public void testBoundsCheck() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length + 1, new StringBuilder(), null));
	}

	@Test
	public void testCharArrayBoundsCheck() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		assertFalse(filter.process(new char[]{}, 0, 1, new StringBuilder(), null));
	}

	@Test
	public void testCharArrayInput() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		String json = "{\"k\":\"v\"}";
		char[] chars = json.toCharArray();
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(chars, 0, chars.length, output, null));
		assertTrue(output.toString().contains("v"));
	}

	@Test
	public void testResizableByteArrayOutputStream() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		String json = "{\"key\":\"value\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertTrue(output.size() > 0);
	}

	@Test
	public void testResizableByteArrayOutputStreamBoundsCheck() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length + 1, new ResizableByteArrayOutputStream(64), null));
	}

	@Test
	public void testNestedObject() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		String json = "{\"a\":{\"b\":\"c\"}}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertTrue(output.toString().contains("c"));
	}

	@Test
	public void testArray() throws Exception {
		SimdJsonMaxStringLengthJsonFilter filter = new SimdJsonMaxStringLengthJsonFilter(-1);
		String json = "[\"a\",\"b\",\"c\"]";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertTrue(output.toString().contains("a"));
	}

}
