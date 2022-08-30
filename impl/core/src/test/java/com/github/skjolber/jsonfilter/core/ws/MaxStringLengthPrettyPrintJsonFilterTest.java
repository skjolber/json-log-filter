package com.github.skjolber.jsonfilter.core.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthPrettyPrintJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthPrettyPrintJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthPrettyPrintJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthPrettyPrintJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthPrettyPrintJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		MaxStringLengthPrettyPrintJsonFilter maxStringLengthJsonFilter = new MaxStringLengthPrettyPrintJsonFilter(-1);
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED));
		assertNull(maxStringLengthJsonFilter.process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		
		assertNull(new MaxStringLengthPrettyPrintJsonFilter(-1).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthPrettyPrintJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH);
	}
	
}
