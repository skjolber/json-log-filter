package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class DefaultJacksonJsonFilterTest {

	@Test
	public void testException() throws Exception {
		
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();
		
		assertNull(filter.process(new byte[] {}, 0, 100));
		assertFalse(filter.process(new char[] {}, 0, 100, new StringBuilder()));
	}
}
