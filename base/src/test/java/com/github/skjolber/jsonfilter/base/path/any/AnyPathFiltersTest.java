package com.github.skjolber.jsonfilter.base.path.any;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.any.AnyPathFilter;

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
	public void testMatchAnySameFirstCharNoMatch() {
		// "dog" has same first char 'd' and same length 3 as "def" → candidates not null but no match
		// This covers: unencodedMatch continue main, matchPath type==null fallthrough
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("def", FilterType.ANON));

		String dog = "dog";
		assertNull(anyPathFilters.matchPath(dog.toCharArray(), 0, 3));
		assertNull(anyPathFilters.matchPath(dog.getBytes(StandardCharsets.UTF_8), 0, 3));
	}

	@Test
	public void testMultipleFiltersWithSameFirstChar() {
		// Two filters "def" and "dog" with same first char 'd' and same length 3
		// This covers fillExact Arrays.copyOf path AND unencodedMatch continue main then find match
		AnyPathFilters anyPathFilters = AnyPathFilters.create(
			AnyPathFilter.create("def", FilterType.ANON),
			AnyPathFilter.create("dog", FilterType.PRUNE)
		);

		// Match second filter
		String dog = "dog";
		assertEquals(FilterType.PRUNE, anyPathFilters.matchPath(dog.toCharArray(), 0, 3));
		assertEquals(FilterType.PRUNE, anyPathFilters.matchPath(dog.getBytes(StandardCharsets.UTF_8), 0, 3));

		// Match first filter
		String def = "def";
		assertEquals(FilterType.ANON, anyPathFilters.matchPath(def.toCharArray(), 0, 3));
		assertEquals(FilterType.ANON, anyPathFilters.matchPath(def.getBytes(StandardCharsets.UTF_8), 0, 3));

		// No match among multiple same-first-char candidates
		String dxy = "dxy";
		assertNull(anyPathFilters.matchPath(dxy.toCharArray(), 0, 3));
		assertNull(anyPathFilters.matchPath(dxy.getBytes(StandardCharsets.UTF_8), 0, 3));
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

	@Test
	public void testAnyPathFilterToString() {
		AnyPathFilter filter = AnyPathFilter.create("mykey", FilterType.ANON);
		String str = filter.toString();
		assertNotNull(str);
		assertTrue(str.contains("mykey"));
	}

	@Test
	public void testEmptyKey() {
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("def", FilterType.ANON));
		assertNull(anyPathFilters.matchPath("".toCharArray(), 0, 0));
		assertNull(anyPathFilters.matchPath("".getBytes(StandardCharsets.UTF_8), 0, 0));
	}

	@Test
	public void testEncodedKeyPrefixTooLong() {
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("ab", FilterType.ANON));

		String longPrefixEsc = "abcd\\u006e"; // 4 plain chars before \\, then unicode escape; readLength=4 >= encodingFilters.length=4
		assertNull(anyPathFilters.matchPath(longPrefixEsc.toCharArray(), 0, longPrefixEsc.length()));
		assertNull(anyPathFilters.matchPath(longPrefixEsc.getBytes(StandardCharsets.UTF_8), 0, longPrefixEsc.length()));
	}

	@Test
	public void testEmptyPathName() {
		AnyPathFilters anyPathFilters = AnyPathFilters.create(AnyPathFilter.create("", FilterType.ANON));
		assertNotNull(anyPathFilters);
		// An empty-name filter won't match any key via matchPath (exact or encoded check)
		assertNull(anyPathFilters.matchPath("".toCharArray(), 0, 0));
		assertNull(anyPathFilters.matchPath("".getBytes(java.nio.charset.StandardCharsets.UTF_8), 0, 0));
	}


}

