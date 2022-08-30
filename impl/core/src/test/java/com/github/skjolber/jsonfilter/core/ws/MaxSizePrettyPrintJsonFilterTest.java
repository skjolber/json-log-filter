package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;

public class MaxSizePrettyPrintJsonFilterTest extends DefaultJsonFilterTest {

	public MaxSizePrettyPrintJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxSizePrettyPrintJsonFilter(size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxSizePrettyPrintJsonFilter(size));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizePrettyPrintJsonFilter filter = new MaxSizePrettyPrintJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ByteArrayOutputStream()));
		
		filter = new MaxSizePrettyPrintJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ByteArrayOutputStream()));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizePrettyPrintJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizePrettyPrintJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizePrettyPrintJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxSizePrettyPrintJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxSizePrettyPrintJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new MaxSizePrettyPrintJsonFilter(DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}
	
}
