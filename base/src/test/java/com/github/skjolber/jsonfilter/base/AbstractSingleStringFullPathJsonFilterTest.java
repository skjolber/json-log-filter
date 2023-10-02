package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractSingleStringFullPathJsonFilterTest {

	private static class MySingleStringPathJsonFilter extends AbstractSingleStringFullPathJsonFilter {

		public MySingleStringPathJsonFilter(int maxStringLength, String expression, FilterType type) {
			super(maxStringLength, -1, -1, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
			return false;
		}
		
		public String[] getPaths() {
			return paths;
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}
	}

	@Test
	public void testFullPathThrowsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MySingleStringPathJsonFilter(-1, "//abc", FilterType.ANON);
		});
	}
	
	@Test
	public void testFullPath() {
		String[] paths = new MySingleStringPathJsonFilter(-1, "/abc", FilterType.ANON).getPaths();
		assertEquals(new String(paths[1]), "abc");
	}
}
