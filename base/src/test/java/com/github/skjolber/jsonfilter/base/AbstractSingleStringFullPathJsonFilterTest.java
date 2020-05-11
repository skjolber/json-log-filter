package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractSingleStringFullPathJsonFilterTest {

	private static class MySingleStringPathJsonFilter extends AbstractSingleStringFullPathJsonFilter {

		public MySingleStringPathJsonFilter(int maxStringLength, String expression, FilterType type) {
			super(maxStringLength, expression, type);
		}
	}

	@Test
	public void testFullPathThrowsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MySingleStringPathJsonFilter(-1, "//abc", FilterType.ANON);
		});
	}
}
