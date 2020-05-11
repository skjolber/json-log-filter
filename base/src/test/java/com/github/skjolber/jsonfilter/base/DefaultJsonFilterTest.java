package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.*;

import java.io.IOException;
import java.io.StringReader;

public class DefaultJsonFilterTest {

	private static final String JSON = "{}";
	
	@Test
	public void testPassthrough() throws IOException {
		
		DefaultJsonFilter filter = new DefaultJsonFilter();

		assertThat(filter.process(JSON)).isEqualTo(JSON);

		StringBuilder builder = new StringBuilder();
		filter.process(JSON, builder);
		assertThat(builder.toString()).isEqualTo(JSON);

		builder.setLength(0);
		filter.process(new StringReader(JSON), 2, builder);
		assertThat(builder.toString()).isEqualTo(JSON);
		
	}
}
