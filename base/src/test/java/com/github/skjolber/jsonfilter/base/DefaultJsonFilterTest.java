package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class DefaultJsonFilterTest {

	private static final String JSON = "{}";
	
	@Test
	public void testPassthrough() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		assertThat(filter.process(JSON)).isEqualTo(JSON);
		
		// char-array
		assertThat(filter.process(JSON.toCharArray())).isEqualTo(JSON);

		StringBuilder builder = new StringBuilder();
		filter.process(getChars(), 0, 2, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
		filter.process(JSON, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		// byte-array
		assertThat(filter.process(getBytes())).isEqualTo(getBytes());
		assertThat(filter.process(getBytes(), 0, 2)).isEqualTo(getBytes());
		
		ResizableByteArrayOutputStream s = new ResizableByteArrayOutputStream(1111);
		filter.process(getBytes(), 0, 2, s);
		assertThat(s.size()).isEqualTo(2);
	}

	@Test
	public void testException() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		// char-array
		StringBuilder builder = new StringBuilder();
		filter.process(JSON, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
	}	
	
	private byte[] getBytes() {
		return JSON.getBytes(StandardCharsets.UTF_8);
	}
	
	private char[] getChars() {
		return JSON.toCharArray();
	}
}
