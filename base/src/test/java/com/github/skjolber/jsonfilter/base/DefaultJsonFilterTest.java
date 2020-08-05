package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

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

		builder.setLength(0);
		filter.process(new StringReader(JSON), 2, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
		filter.process(new StringReader(JSON), -1, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		// byte-array
		assertThat(filter.process(getBytes())).isEqualTo(getBytes());

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		filter.process(getBytes(), 0, 2, outputStream);
		assertThat(outputStream.toString()).isEqualTo(JSON);

		outputStream.reset();
		filter.process(getBytes(), outputStream);
		assertThat(outputStream.toString()).isEqualTo(JSON);

		outputStream.reset();
		filter.process(new ByteArrayInputStream(getBytes()), 2, outputStream);
		assertThat(outputStream.toString()).isEqualTo(JSON);

		outputStream.reset();
		filter.process(new ByteArrayInputStream(getBytes()), -1, outputStream);
		assertThat(outputStream.toString()).isEqualTo(JSON);
		
	}

	@Test
	public void testException() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		// char-array
		StringBuilder builder = new StringBuilder();
		filter.process(JSON, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
		
		assertThrows(EOFException.class,
				() -> filter.process(new StringReader(JSON), 4, builder));
		
		// byte-array
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		filter.process(getBytes(), outputStream);
		assertThat(outputStream.toString()).isEqualTo(JSON);

		outputStream.reset();
		
		assertThrows(EOFException.class,
				() -> filter.process(new ByteArrayInputStream(getBytes()), 4, outputStream));
		
	}	
	
	private byte[] getBytes() {
		return JSON.getBytes(StandardCharsets.UTF_8);
	}
	
	private char[] getChars() {
		return JSON.toCharArray();
	}
}
