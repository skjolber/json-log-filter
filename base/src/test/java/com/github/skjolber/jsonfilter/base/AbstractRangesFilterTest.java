package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractRangesFilterTest {

	public class MyAbstractRangesFilter extends AbstractRangesFilter {

		public MyAbstractRangesFilter(int initialCapacity, int length) {
			super(initialCapacity, length);
		}
	}

	@Test
	public void testConstructor() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractRangesFilter(0, 1024);
		});
	}

	@Test
	public void testAdd() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		filter.addMaxLength(0, 1, 2);
		filter.addAnon(3, 4);
		filter.addPrune(5, 6);
		
		assertThat(filter.getFilterIndex()).isEqualTo(9);
	}
	
	@Test
	public void testDefaultCapacity() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(-1, 1024);
		assertThat(filter.getFilter().length).isEqualTo(AbstractRangesFilter.DEFAULT_INITIAL_ARRAY_SIZE * 3);
	}
	
	@Test
	public void testExtendsCapacity() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(2, 1024);
		filter.addMaxLength(0, 1, 2);
		filter.addAnon(3, 4);
		filter.addPrune(5, 6);
		assertThat(filter.getFilter().length).isEqualTo(12);
	}
}
