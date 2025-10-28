package com.github.skjolber.jsonfilter.jackson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class DefaultJacksonJsonFilterTest {

	@Test
	public void testException() throws Exception {
		
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter();
		
		assertNull(filter.process(new byte[] {}, 0, 100));
		assertFalse(filter.process(new char[] {}, 0, 100, new StringBuilder()));
		
		String illegalJson = "{abcdef}";
		assertFalse(filter.process(illegalJson.toCharArray(), 0, 3, new StringBuilder()));
		assertFalse(filter.process(illegalJson.getBytes(StandardCharsets.UTF_8), 0, 3, new ResizableByteArrayOutputStream(1024)));
	}
	
	@Test
	public void testExceptionConstructor() throws Exception {
		
		JsonFactory factory = new JsonFactory();
		DefaultJacksonJsonFilter filter = new DefaultJacksonJsonFilter(factory);
		
		assertNull(filter.process(new byte[] {}, 0, 100));
		assertFalse(filter.process(new char[] {}, 0, 100, new StringBuilder()));
	}
}
