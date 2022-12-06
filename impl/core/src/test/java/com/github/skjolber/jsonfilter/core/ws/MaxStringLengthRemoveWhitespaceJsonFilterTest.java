package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthRemoveWhitespaceJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthRemoveWhitespaceJsonFilterTest() throws Exception {
		super(true);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MaxStringLengthRemoveWhitespaceJsonFilter maxStringLengthJsonFilter = new MaxStringLengthRemoveWhitespaceJsonFilter(-1);
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED));
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertNull(new MaxStringLengthRemoveWhitespaceJsonFilter(-1).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthRemoveWhitespaceJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
}
