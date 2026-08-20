package com.github.skjolber.jsonfilter.simdjson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class SimdJsonAnyPathMaxStringLengthJsonFilterTest {

	@Test
	public void testAnonymize() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..password"}, null);
		String json = "{\"user\":\"alice\",\"password\":\"secret\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertTrue(result.contains("alice"));
		assertFalse(result.contains("secret"));
	}

	@Test
	public void testPrune() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, null, new String[]{"$..sensitive"});
		String json = "{\"name\":\"bob\",\"sensitive\":\"data\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertTrue(result.contains("bob"));
		assertFalse(result.contains("data"));
	}

	@Test
	public void testConstructorRejectsFullPath() {
		assertThrows(IllegalArgumentException.class, () -> new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"/a/b"}, null));
	}

	@Test
	public void testInvalidJson() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..key"}, null);
		byte[] bytes = "{invalid".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length, new StringBuilder(), null));
	}

	@Test
	public void testBoundsCheck() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..key"}, null);
		byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(bytes, 0, bytes.length + 1, new StringBuilder(), null));
	}

	@Test
	public void testCharArrayBoundsCheck() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..key"}, null);
		assertFalse(filter.process(new char[]{}, 0, 1, new StringBuilder(), null));
	}

	@Test
	public void testResizableByteArrayOutputStream() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..password"}, null);
		String json = "{\"password\":\"secret\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(64);
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
		assertFalse(result.contains("secret"));
	}

	@Test
	public void testMaxStringLength() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				3, new String[]{"$..password"}, null);
		String json = "{\"password\":\"secret\",\"note\":\"longervalue\"}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		String result = output.toString();
		assertFalse(result.contains("longervalue"));
	}

	@Test
	public void testAnonymizeObject() throws Exception {
		SimdJsonAnyPathMaxStringLengthJsonFilter filter = new SimdJsonAnyPathMaxStringLengthJsonFilter(
				-1, new String[]{"$..nested"}, null);
		String json = "{\"nested\":{\"a\":\"b\"}}";
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		StringBuilder output = new StringBuilder();
		assertTrue(filter.process(bytes, 0, bytes.length, output, null));
		assertNotNull(output.toString());
	}

}
