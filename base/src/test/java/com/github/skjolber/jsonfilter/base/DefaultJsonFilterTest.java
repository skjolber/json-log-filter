package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;

public class DefaultJsonFilterTest {

	private static final String JSON = "{}";
	
	@Test
	public void testPassthrough() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		assertThat(filter.process(JSON)).isEqualTo(JSON);
		assertThat(filter.process(JSON.toCharArray())).isEqualTo(JSON);

		StringBuilder builder = new StringBuilder();
		filter.process(JSON.toCharArray(), 0, 2, builder);
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

	}
	
	@Test
	public void testException() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		assertThat(filter.process(JSON)).isEqualTo(JSON);

		StringBuilder builder = new StringBuilder();
		filter.process(JSON, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
		
		assertThrows(EOFException.class,
				() -> filter.process(new StringReader(JSON), 4, builder));
		
		
	}	
}
