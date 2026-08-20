package com.github.skjolber.jsonfilter.simdjson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class SimdJsonPathMaxStringLengthJsonFilterTest {

	@Test
	public void testAnonymize() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(
				-1, new String[]{"/user/password"}, null);
		String json = "{\"user\":{\"password\":\"secret\",\"name\":\"alice\"}}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertTrue(result.contains("alice"));
		assertFalse(result.contains("secret"));
	}

	@Test
	public void testPrune() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(
				-1, null, new String[]{"/data/sensitive"});
		String json = "{\"data\":{\"sensitive\":\"hidden\",\"visible\":\"ok\"}}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertTrue(result.contains("ok"));
		assertFalse(result.contains("hidden"));
	}

	@Test
	public void testPassthrough() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(-1, null, null);
		String json = "{\"key\":\"value\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertTrue(output.toString().contains("value"));
	}

	@Test
	public void testInvalidJson() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(
				-1, new String[]{"/a"}, null);
		byte[] bytes = "{invalid".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length, new StringBuilder(), null));
	}

	@Test
	public void testBoundsCheck() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(-1, null, null);
		byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length + 1, new StringBuilder(), null));
	}

	@Test
	public void testCharArrayBoundsCheck() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(-1, null, null);
		assertFalse(filter.process(new char[]{}, 0, 1, new StringBuilder(), null));
	}

	@Test
	public void testResizableByteArrayOutputStream() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(
				-1, new String[]{"/password"}, null);
		String json = "{\"password\":\"secret\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertNotNull(output.toByteArray());
	}

	@Test
	public void testMaxStringLength() throws Exception {
		SimdJsonPathMaxStringLengthJsonFilter filter = new SimdJsonPathMaxStringLengthJsonFilter(
				3, null, null);
		String json = "{\"key\":\"longervalue\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertFalse(output.toString().contains("longervalue"));
	}

}
