package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

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
	
	@Test
	public void addLengthToDigits() {
		assertEquals(1, AbstractRangesFilter.lengthToDigits(0));
		assertEquals(2, AbstractRangesFilter.lengthToDigits(10));
		assertEquals(3, AbstractRangesFilter.lengthToDigits(100));
		assertEquals(4, AbstractRangesFilter.lengthToDigits(1000));
		assertEquals(5, AbstractRangesFilter.lengthToDigits(10000));
		assertEquals(6, AbstractRangesFilter.lengthToDigits(100000));
		assertEquals(7, AbstractRangesFilter.lengthToDigits(1000000));
		assertEquals(8, AbstractRangesFilter.lengthToDigits(10000000));
		assertEquals(9, AbstractRangesFilter.lengthToDigits(100000000));
		assertEquals(10, AbstractRangesFilter.lengthToDigits(1000000000));
	}

	@Test
	public void testAddWithFilterTypePrune() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		filter.add(FilterType.PRUNE, 0, 1);
		assertThat(filter.getFilterIndex()).isEqualTo(3);
	}

	@Test
	public void testAddWithFilterTypeAnon() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		filter.add(FilterType.ANON, 2, 3);
		assertThat(filter.getFilterIndex()).isEqualTo(3);
	}

	@Test
	public void testAddWithFilterTypeDeleteDoesNothing() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		int before = filter.getFilterIndex();
		filter.add(FilterType.DELETE, 4, 5);
		assertThat(filter.getFilterIndex()).isEqualTo(before);
	}

	@Test
	public void testAddDelete() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		filter.addDelete(10, 20);
		assertThat(filter.getFilterIndex()).isEqualTo(3);
	}

	@Test
	public void testRemoveLastFilter() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1024);
		filter.addAnon(0, 1);
		filter.addPrune(2, 3);
		assertThat(filter.getFilterIndex()).isEqualTo(6);
		filter.removeLastFilter();
		assertThat(filter.getFilterIndex()).isEqualTo(3);
	}

	@Test
	public void testGetMaxOutputLength() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1000);
		assertThat(filter.getMaxOutputLength()).isEqualTo(1000);
	}

	@Test
	public void testGetRemovedLength() {
		MyAbstractRangesFilter filter = new MyAbstractRangesFilter(1024, 1000);
		assertThat(filter.getRemovedLength()).isEqualTo(0);
	}
}
