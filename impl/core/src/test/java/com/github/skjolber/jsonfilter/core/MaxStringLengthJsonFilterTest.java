package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class MaxStringLengthJsonFilterTest  extends DefaultJsonFilterTest {

	public MaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new MaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new byte[] {}, 1, 1, new ByteArrayOutputStream()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new MaxStringLengthJsonFilter(-1).process(TRUNCATED));
		assertFalse(new MaxStringLengthJsonFilter(-1).process(FULL, 0, FULL.length - 3, new StringBuilder()));
		
		assertNull(new MaxStringLengthJsonFilter(-1).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new String(FULL).getBytes(StandardCharsets.UTF_8), 0, FULL.length - 3, new ByteArrayOutputStream()));
		
	}
	
	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
}
