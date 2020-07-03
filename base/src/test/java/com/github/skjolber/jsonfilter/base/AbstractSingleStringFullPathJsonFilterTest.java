package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractSingleStringFullPathJsonFilterTest {

	private static class MySingleStringPathJsonFilter extends AbstractSingleStringFullPathJsonFilter {

		public MySingleStringPathJsonFilter(int maxStringLength, String expression, FilterType type) {
			super(maxStringLength, -1, expression, type, FILTER_PRUNE_MESSAGE, FILTER_ANONYMIZE, FILTER_TRUNCATE_MESSAGE);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
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
