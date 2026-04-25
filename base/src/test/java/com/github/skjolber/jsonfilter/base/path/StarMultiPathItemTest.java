package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.path.SinglePathItem;
import com.github.skjolber.jsonfilter.base.path.StarMultiPathItem;

public class StarMultiPathItemTest extends AbstractPathTest {

	@Test
	public void test() {
		SinglePathItem p = new SinglePathItem(0, "a", null);
		
		SinglePathItem star = new SinglePathItem(2, "c", null);
		
		StarMultiPathItem item = new StarMultiPathItem(Arrays.asList("a", "b"), 0, null);
		item.setAny(star);
		item.setNext(p, 0);
		item.setNext(p, 1);

		// string
		assertSame(p, item.matchPath(0, "a"));
		assertSame(p, item.matchPath(0, "b"));
		assertSame(star, item.matchPath(0, "d"));
		
		// char
		assertSame(p, item.matchPath(0, aChars, 0, 1));
		assertSame(p, item.matchPath(0, bChars, 0, 1));
		assertSame(star, item.matchPath(0, dChars, 0, 1));
		
		// byte
		assertSame(p, item.matchPath(0, aBytes, 0, 1));
		assertSame(p, item.matchPath(0, bBytes, 0, 1));
		assertSame(star, item.matchPath(0, dBytes, 0, 1));
	}

	@Test
	public void testLevelMismatch() {
		SinglePathItem p = new SinglePathItem(0, "a", null);
		SinglePathItem star = new SinglePathItem(2, "c", null);
		StarMultiPathItem item = new StarMultiPathItem(Arrays.asList("a", "b"), 0, null);
		item.setAny(star);
		item.setNext(p, 0);
		item.setNext(p, 1);

		// level mismatch -> returns self
		assertSame(item, item.matchPath(1, "a"));
		assertSame(item, item.matchPath(1, aBytes, 0, 1));
		assertSame(item, item.matchPath(1, aChars, 0, 1));
	}

	@Test
	public void testEmptyKey() {
		SinglePathItem p = new SinglePathItem(0, "a", null);
		SinglePathItem star = new SinglePathItem(1, "x", null);
		StarMultiPathItem item = new StarMultiPathItem(Arrays.asList("a", "b"), 0, null);
		item.setAny(star);
		item.setNext(p, 0);
		item.setNext(p, 1);

		// empty key -> slow path -> no match -> returns any
		assertSame(star, item.matchPath(0, new byte[0], 0, 0));
		assertSame(star, item.matchPath(0, new char[0], 0, 0));
	}

	/**
	 * Verifies that a JSON Unicode escape sequence at any offset in a JSON key is matched
	 * correctly, and that unrecognised encoded keys fall through to the wildcard item.
	 */
	@Test
	public void testEncodedKeys() {
		SinglePathItem p    = new SinglePathItem(0, "name", null);
		SinglePathItem star = new SinglePathItem(1, "*", null);

		StarMultiPathItem item = new StarMultiPathItem(Arrays.asList("name"), 0, null);
		item.setNext(p, 0);
		item.setAny(star);

		// raw "\\u006eame" has 'n' encoded at the first position; decodes to "name"
		byte[] firstEscBytes = "\\u006eame".getBytes(StandardCharsets.UTF_8);
		char[] firstEscChars = "\\u006eame".toCharArray();
		// raw "na\\u006de" has 'm' (0x006d) encoded at the middle position; decodes to "name"
		byte[] midEscBytes   = "na\\u006de".getBytes(StandardCharsets.UTF_8);
		char[] midEscChars   = "na\\u006de".toCharArray();
		// raw "nam\\u0065" has 'e' (0x0065) encoded at the last position; decodes to "name"
		byte[] lastEscBytes  = "nam\\u0065".getBytes(StandardCharsets.UTF_8);
		char[] lastEscChars  = "nam\\u0065".toCharArray();
		// raw "na\\u006fe" has 'o' (0x006f) encoded; decodes to "naoe", not "name"
		byte[] noMatchBytes  = "na\\u006fe".getBytes(StandardCharsets.UTF_8);
		char[] noMatchChars  = "na\\u006fe".toCharArray();

		assertSame(p,    item.matchPath(0, firstEscBytes,  0, firstEscBytes.length));
		assertSame(p,    item.matchPath(0, firstEscChars,  0, firstEscChars.length));
		assertSame(p,    item.matchPath(0, midEscBytes,    0, midEscBytes.length));
		assertSame(p,    item.matchPath(0, midEscChars,    0, midEscChars.length));
		assertSame(p,    item.matchPath(0, lastEscBytes,   0, lastEscBytes.length));
		assertSame(p,    item.matchPath(0, lastEscChars,   0, lastEscChars.length));
		// non-matching encoded key → falls through to wildcard
		assertSame(star, item.matchPath(0, noMatchBytes,   0, noMatchBytes.length));
		assertSame(star, item.matchPath(0, noMatchChars,   0, noMatchChars.length));
	}
}
