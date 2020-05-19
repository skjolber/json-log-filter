package com.github.skjolber.jsonfilter.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AbstractSingleCharArrayAnyPathJsonFilterTest {

	private static class MySingleCharArrayAnyPathJsonFilter extends AbstractSingleCharArrayAnyPathJsonFilter {

		public MySingleCharArrayAnyPathJsonFilter(int maxStringLength, String expression, FilterType type) {
			super(maxStringLength, -1, expression, type);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			// TODO Auto-generated method stub
			return false;
		}

	}

	@Test
	public void testFullPathThrowsException() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MySingleCharArrayAnyPathJsonFilter(-1, "/abc", FilterType.ANON);
		});
	}
}
