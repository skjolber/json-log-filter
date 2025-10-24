package com.github.skjolber.jsonfilter.base.path.any;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class AnyPathFiltersTest {

	@Test
	public void testMatchAny() {
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("def", FilterType.ANON));
		
		String def = "def";
		assertEquals(anyPathFilters.matchPath(def), FilterType.ANON);
		assertEquals(anyPathFilters.matchPath(def.toCharArray(), 0, 3), FilterType.ANON);
		assertEquals(anyPathFilters.matchPath(def.getBytes(StandardCharsets.UTF_8), 0, 3), FilterType.ANON);

		String de = "de";
		assertNull(anyPathFilters.matchPath(de));
		assertNull(anyPathFilters.matchPath(de.toCharArray(), 0, 2));
		assertNull(anyPathFilters.matchPath(de.getBytes(StandardCharsets.UTF_8), 0, 2));

		String defgh = "defgh";
		assertNull(anyPathFilters.matchPath(defgh));
		assertNull(anyPathFilters.matchPath(defgh.toCharArray(), 0, 5));
		assertNull(anyPathFilters.matchPath(defgh.getBytes(StandardCharsets.UTF_8), 0, 5));
		
		String fgh = "fgh";
		assertNull(anyPathFilters.matchPath(fgh));
		assertNull(anyPathFilters.matchPath(fgh.toCharArray(), 0, 3));
		assertNull(anyPathFilters.matchPath(fgh.getBytes(StandardCharsets.UTF_8), 0, 3));
	}

	@Test
	public void testMatchEscaped() {
		// note: does not have to be a smiley, all chars can be encoded using \\u
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("xx😀yy", FilterType.ANON));
		
		String def = "xx\\uD83D\\uDE00yy";
		char[] charArray = def.toCharArray();
		assertEquals(anyPathFilters.matchPath(charArray, 0, charArray.length), FilterType.ANON);
		
		byte[] bytes = def.getBytes(StandardCharsets.UTF_8);
		assertEquals(anyPathFilters.matchPath(bytes, 0, bytes.length), FilterType.ANON);
	}
	
}
