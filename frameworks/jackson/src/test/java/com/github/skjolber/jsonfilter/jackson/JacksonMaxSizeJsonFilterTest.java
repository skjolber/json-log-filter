package com.github.skjolber.jsonfilter.jackson;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class JacksonMaxSizeJsonFilterTest extends AbstractJacksonJsonFilterTest {

	public JacksonMaxSizeJsonFilterTest() throws Exception {
		super(false);
	}

	@Test
	public void testMaxSize() throws IOException {
		validate("/json/maxSize/cve2006.json.gz.json", (size) -> new JacksonMaxSizeJsonFilter(size));
	}

	@Test
	public void passthrough_success() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(-1)).hasPassthrough();
	}

	@Test
	public void exception_returns_false() throws Exception {
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new char[] {}, 1, 1, new StringBuilder()));
		assertFalse(new JacksonMaxSizeJsonFilter(-1).process(new byte[] {}, 1, 1, new StringBuilder()));
	}

	@Test
	public void exception_offset_if_not_exceeded() throws Exception {
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED));
		assertNull(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE).process(TRUNCATED.getBytes(StandardCharsets.UTF_8)));
	}
	
	@Test
	public void maxSize() throws Exception {
		assertThat(new JacksonMaxSizeJsonFilter(DEFAULT_MAX_SIZE)).hasMaxSize(DEFAULT_MAX_SIZE);
	}
	
}
