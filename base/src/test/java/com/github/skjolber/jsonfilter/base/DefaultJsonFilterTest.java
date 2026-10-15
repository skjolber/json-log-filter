package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

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
	public void testMetrics() throws IOException {
		DefaultJsonFilter filter = new DefaultJsonFilter();

		// the document within a larger array
		char[] chars = ("[" + JSON + "]").toCharArray();
		byte[] bytes = ("[" + JSON + "]").getBytes(StandardCharsets.UTF_8);

		assertSizeMetrics((metrics) -> filter.process(JSON, metrics));
		assertSizeMetrics((metrics) -> filter.process(JSON, new StringBuilder(), metrics));
		assertSizeMetrics((metrics) -> filter.process(getChars(), metrics));
		assertSizeMetrics((metrics) -> filter.process(chars, 1, 2, new StringBuilder(), metrics));
		assertSizeMetrics((metrics) -> filter.process(getBytes(), metrics));
		assertSizeMetrics((metrics) -> filter.process(bytes, 1, 2, metrics));
		assertSizeMetrics((metrics) -> filter.process(bytes, 1, 2, new ResizableByteArrayOutputStream(16), metrics));
	}

	private static void assertSizeMetrics(Consumer<DefaultJsonFilterMetrics> process) {
		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		process.accept(metrics);
		assertThat(metrics.getInputSize()).isEqualTo(JSON.length());
		assertThat(metrics.getOutputSize()).isEqualTo(JSON.length());
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
