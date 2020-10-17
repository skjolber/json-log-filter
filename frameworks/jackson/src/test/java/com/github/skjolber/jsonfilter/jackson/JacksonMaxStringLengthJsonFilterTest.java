package com.github.skjolber.jsonfilter.jackson;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class JacksonMaxStringLengthJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMaxStringLengthJsonFilterTest() throws Exception {
		super();
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void maxStringLength() throws Exception {
		assertThat(new JacksonMaxStringLengthJsonFilter(DEFAULT_MAX_LENGTH)).hasMaxStringLength(DEFAULT_MAX_LENGTH);
	}
	
	@Test
	public void testConvenienceMethods() throws IOException {
		testConvenienceMethods(new JacksonMaxStringLengthJsonFilter(-1));
	}
	
}
