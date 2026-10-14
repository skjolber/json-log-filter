package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * The output buffer for a slice of a byte array is sized from the slice, not the whole array.
 */

public class AbstractJsonFilterSliceTest {

	private static class SliceJsonFilter extends AbstractJsonFilter {

		private int initialBufferSize = -1;

		public SliceJsonFilter() {
			super(-1, -1, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output, JsonFilterMetrics filterMetrics) {
			output.append(chars, offset, length);
			return true;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output, JsonFilterMetrics filterMetrics) {
			initialBufferSize = output.getBuffer().length;
			output.write(chars, offset, length);
			return true;
		}
	}

	private static final String JSON = "{\"a\":1}";

	@Test
	public void bufferIsSizedFromSlice() {
		byte[] json = JSON.getBytes(StandardCharsets.UTF_8);
		byte[] bytes = new byte[64 * 1024];
		int offset = 1000;
		System.arraycopy(json, 0, bytes, offset, json.length);

		SliceJsonFilter filter = new SliceJsonFilter();

		assertThat(filter.process(bytes, offset, json.length)).isEqualTo(json);
		assertThat(filter.initialBufferSize).isEqualTo(json.length);

		assertThat(filter.process(bytes, offset, json.length, new DefaultJsonFilterMetrics())).isEqualTo(json);
		assertThat(filter.initialBufferSize).isEqualTo(json.length);

		assertThat(filter.process(bytes, offset, json.length, (JsonFilterMetrics)null)).isEqualTo(json);
		assertThat(filter.initialBufferSize).isEqualTo(json.length);
	}

	@Test
	public void bufferIsSizedFromWholeArray() {
		byte[] json = JSON.getBytes(StandardCharsets.UTF_8);

		SliceJsonFilter filter = new SliceJsonFilter();

		assertThat(filter.process(json)).isEqualTo(json);
		assertThat(filter.initialBufferSize).isEqualTo(json.length);

		assertThat(filter.process(json, new DefaultJsonFilterMetrics())).isEqualTo(json);
		assertThat(filter.initialBufferSize).isEqualTo(json.length);

		assertThat(filter.process(JSON.toCharArray(), new DefaultJsonFilterMetrics())).isEqualTo(JSON);
	}
}
