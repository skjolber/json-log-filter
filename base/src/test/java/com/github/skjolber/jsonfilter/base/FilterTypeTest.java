package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class FilterTypeTest {

	@Test
	public void pruneTakesPrecedence() {
		assertEquals(FilterType.PRUNE, FilterType.strongest(FilterType.ANON, FilterType.PRUNE));
		assertEquals(FilterType.PRUNE, FilterType.strongest(FilterType.PRUNE, FilterType.ANON));
		assertEquals(FilterType.ANON, FilterType.strongest(FilterType.ANON, FilterType.ANON));
	}

	@Test
	public void nullIsWeakest() {
		assertEquals(FilterType.ANON, FilterType.strongest(null, FilterType.ANON));
		assertEquals(FilterType.ANON, FilterType.strongest(FilterType.ANON, null));
		assertNull(FilterType.strongest(null, null));
	}
}
