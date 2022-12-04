package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.github.skjolber.jsonfilter.base.DefaultJsonFilter;
import com.github.skjolber.jsonfilter.core.MaxSizeJsonFilter;
import com.github.skjolber.jsonfilter.core.pp.Indent;
import com.github.skjolber.jsonfilter.core.pp.PrettyPrintingJsonFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;
import com.github.skjolber.jsonfilter.test.Generator;
import com.github.skjolber.jsonfilter.test.MaxSizeJsonFilterAdapter;

public class MaxSizeRemoveWhitespaceJsonFilterTest extends DefaultJsonFilterTest {

	private final static PrettyPrintingJsonFilter pp = new PrettyPrintingJsonFilter(Indent.newBuilder().build());

	public MaxSizeRemoveWhitespaceJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	@ResourceLock(value = "jackson")
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size));
	}

	@Test
	public void testDeepStructure() throws IOException {
		validateDeepStructure( (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size));
	}

	@Test
	public void testInvalidInput() throws Exception {
		String string = new String(Generator.generateDeepObjectStructure(1000, true), StandardCharsets.UTF_8);

		String broken = string.substring(0, string.length() / 2);
		
		MaxSizeRemoveWhitespaceJsonFilter filter = new MaxSizeRemoveWhitespaceJsonFilter(string.length());

		char[] brokenChars = broken.toCharArray();
		assertFalse(filter.process(brokenChars, 0, string.length(), new StringBuilder()));
		
		byte[] brokenBytes = broken.getBytes(StandardCharsets.UTF_8);
		assertFalse(filter.process(brokenBytes, 0, string.length(), new ByteArrayOutputStream()));
		
		filter = new MaxSizeRemoveWhitespaceJsonFilter(brokenBytes.length);

		assertFalse(filter.process(new char[]{}, 0, string.length(), new StringBuilder()));
		
		assertFalse(filter.process(new byte[]{}, 0, string.length(), new ByteArrayOutputStream()));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxSizeRemoveWhitespaceJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxSizeRemoveWhitespaceJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxSizeRemoveWhitespaceJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new MaxSizeRemoveWhitespaceJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		MaxSizeJsonFilterAdapter maxSize = (size) -> new MaxSizeRemoveWhitespaceJsonFilter(size);
		assertThatMaxSize(maxSize, new RemoveWhitespaceJsonFilter()).hasMaxSize().hasMaxSizeMetrics();
	}
	
}
