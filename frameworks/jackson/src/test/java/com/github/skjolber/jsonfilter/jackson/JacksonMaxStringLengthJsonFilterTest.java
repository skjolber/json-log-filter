package com.github.skjolber.jsonfilter.jackson;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.test.DefaultJsonFilterTest;

public class JacksonMaxStringLengthJsonFilterTest extends DefaultJsonFilterTest {

	public JacksonMaxStringLengthJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
}
