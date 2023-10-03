package com.github.skjolber.jsonfilter.core;

import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
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
		assertFalse(new MaxStringLengthJsonFilter(-1).process(new byte[] {}, 1, 1, new ResizableByteArrayOutputStream(128)));
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new MaxStringLengthJsonFilter(DEFAULT_MAX_STRING_LENGTH)).hasMaxStringLength(DEFAULT_MAX_STRING_LENGTH).hasMaxStringLengthMetrics();
	}
	
}
