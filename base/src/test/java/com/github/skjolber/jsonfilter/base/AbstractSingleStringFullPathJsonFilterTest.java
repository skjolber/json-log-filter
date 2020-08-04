package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractSingleStringFullPathJsonFilterTest {

	private static class MySingleStringPathJsonFilter extends AbstractSingleStringFullPathJsonFilter {

		public MySingleStringPathJsonFilter(int maxStringLength, String expression, FilterType type) {
			super(maxStringLength, -1, expression, type, FILTER_PRUNE_MESSAGE_JSON, FILTER_ANONYMIZE_JSON, FILTER_TRUNCATE_MESSAGE);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
			return false;
		}
		
	}

	@Test
	public void testFullPathThrowsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MySingleStringPathJsonFilter(-1, "//abc", FilterType.ANON);
		});
	}
}
