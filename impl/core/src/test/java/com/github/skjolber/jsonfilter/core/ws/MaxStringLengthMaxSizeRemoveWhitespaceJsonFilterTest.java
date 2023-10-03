package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.cache.MaxSizeJsonFilterPair.MaxSizeJsonFilterFunction;

public class MaxStringLengthMaxSizeRemoveWhitespaceJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthMaxSizeRemoveWhitespaceJsonFilterTest() throws Exception {
		super();
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, size));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizeRemoveWhitespaceJsonFilter filter = new MaxSizeRemoveWhitespaceJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ResizableByteArrayOutputStream(128)));
		
		filter = new MaxSizeRemoveWhitespaceJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, -1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(-1, DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}

	@Test
	public void maxStringLength() throws Exception {
		MaxSizeJsonFilterFunction maxSize = (size) -> new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, size);
		
		assertThatMaxSize(maxSize, new MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH, -1)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}

}
