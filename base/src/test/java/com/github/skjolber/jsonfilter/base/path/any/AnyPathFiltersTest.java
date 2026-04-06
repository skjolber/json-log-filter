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

	// note: does not have to be a smiley, all chars can be encoded using \\u
	@Test
	public void testMatchEscaped() {
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("xx😀yy", FilterType.ANON));
		
		String def = "xx\\uD83D\\uDE00yy";
		char[] charArray = def.toCharArray();
		assertEquals(anyPathFilters.matchPath(charArray, 0, charArray.length), FilterType.ANON);
		
		byte[] bytes = def.getBytes(StandardCharsets.UTF_8);
		assertEquals(anyPathFilters.matchPath(bytes, 0, bytes.length), FilterType.ANON);
	}

	/**
	 * Verifies that lowercase hex digits in a JSON Unicode escape sequence are accepted.
	 * RFC 8259 §7 states "The hexadecimal letters A through F can be upper or lower case."
	 * Tests escapes at mid-key and end-of-key positions, plus a non-matching case.
	 */
	@Test
	public void testMatchEscapedMidKey() {
		AnyPathFilters f = AnyPathFilters.create(AnyPathFilter.create("name", FilterType.ANON));

		// middle char encoded with lowercase hex: "na\\u006de" decodes to "name" ('m' = 0x006d)
		String midEsc = "na\\u006de";
		assertEquals(FilterType.ANON, f.matchPath(midEsc.toCharArray(), 0, midEsc.length()));
		assertEquals(FilterType.ANON, f.matchPath(midEsc.getBytes(StandardCharsets.UTF_8), 0, midEsc.length()));

		// last char encoded with lowercase hex: "nam\\u0065" decodes to "name" ('e' = 0x0065)
		String lastEsc = "nam\\u0065";
		assertEquals(FilterType.ANON, f.matchPath(lastEsc.toCharArray(), 0, lastEsc.length()));
		assertEquals(FilterType.ANON, f.matchPath(lastEsc.getBytes(StandardCharsets.UTF_8), 0, lastEsc.length()));

		// encoded key that decodes to "naoe" (not "name") — must not match
		String noMatch = "na\\u006fe";
		assertNull(f.matchPath(noMatch.toCharArray(), 0, noMatch.length()));
		assertNull(f.matchPath(noMatch.getBytes(StandardCharsets.UTF_8), 0, noMatch.length()));
	}

}

