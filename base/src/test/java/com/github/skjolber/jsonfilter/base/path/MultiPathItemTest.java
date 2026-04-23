package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.path.MultiPathItem;
import com.github.skjolber.jsonfilter.base.path.SinglePathItem;

public class MultiPathItemTest {

	@Test
	public void test() {
		SinglePathItem p = new SinglePathItem(0, "a", null);
		
		MultiPathItem item = new MultiPathItem(Arrays.asList("a", "b"), 0, null);
		item.setNext(p, 0);
		item.setNext(p, 1);
		
		assertSame(p, item.matchPath(0, "a"));
		assertSame(p, item.matchPath(0, "b"));
		
		assertSame(item, item.matchPath(0, "x"));
	}

	@Test
	public void testLevelMismatch() {
		SinglePathItem p = new SinglePathItem(0, "a", null);
		MultiPathItem item = new MultiPathItem(Arrays.asList("name", "my"), 0, null);
		item.setNext(p, 0);
		item.setNext(p, 1);

		// level mismatch -> returns self
		assertSame(item, item.matchPath(1, "name"));
		assertSame(item, item.matchPath(1, "name".getBytes(StandardCharsets.UTF_8), 0, 4));
		assertSame(item, item.matchPath(1, "name".toCharArray(), 0, 4));
	}

	@Test
	public void testFastPathNullCandidates() {
		// Key starts with a char ('z') not in the dispatch table of "name"/"my"
		SinglePathItem p = new SinglePathItem(0, "a", null);
		MultiPathItem item = new MultiPathItem(Arrays.asList("name", "my"), 0, null);
		item.setNext(p, 0);
		item.setNext(p, 1);

		assertSame(item, item.matchPath(0, "xyz".getBytes(StandardCharsets.UTF_8), 0, 3));
		assertSame(item, item.matchPath(0, "xyz".toCharArray(), 0, 3));
	}

	@Test
	public void testFastPathMatchFound() {
		// Direct byte/char match via fast path
		SinglePathItem p = new SinglePathItem(0, "a", null);
		MultiPathItem item = new MultiPathItem(Arrays.asList("name", "my"), 0, null);
		item.setNext(p, 0);
		item.setNext(p, 1);

		assertSame(p, item.matchPath(0, "name".getBytes(StandardCharsets.UTF_8), 0, 4));
		assertSame(p, item.matchPath(0, "name".toCharArray(), 0, 4));
		assertSame(p, item.matchPath(0, "my".getBytes(StandardCharsets.UTF_8), 0, 2));
		assertSame(p, item.matchPath(0, "my".toCharArray(), 0, 2));
	}

	/**
	 * Verifies that a JSON Unicode escape sequence at any offset in a JSON key is matched
	 * correctly. Tests each character position of "name" encoded, and a non-matching case.
	 */
	@Test
	public void testEncodedKeys() {
		SinglePathItem p = new SinglePathItem(0, "name", null);
		MultiPathItem item = new MultiPathItem(Arrays.asList("name", "my"), 0, null);
		item.setNext(p, 0);

		// raw "\\u006eame" has 'n' encoded at the first position; decodes to "name"
		byte[] firstEscBytes  = "\\u006eame".getBytes(StandardCharsets.UTF_8);
		char[] firstEscChars  = "\\u006eame".toCharArray();
		// raw "na\\u006de" has 'm' (0x006d) encoded at the middle position; decodes to "name"
		byte[] midEscBytes    = "na\\u006de".getBytes(StandardCharsets.UTF_8);
		char[] midEscChars    = "na\\u006de".toCharArray();
		// raw "nam\\u0065" has 'e' (0x0065) encoded at the last position; decodes to "name"
		byte[] lastEscBytes   = "nam\\u0065".getBytes(StandardCharsets.UTF_8);
		char[] lastEscChars   = "nam\\u0065".toCharArray();
		// raw "na\\u006fe" has 'o' (0x006f) encoded; decodes to "naoe", not "name"
		byte[] noMatchBytes   = "na\\u006fe".getBytes(StandardCharsets.UTF_8);
		char[] noMatchChars   = "na\\u006fe".toCharArray();

		assertSame(p,    item.matchPath(0, firstEscBytes,  0, firstEscBytes.length));
		assertSame(p,    item.matchPath(0, firstEscChars,  0, firstEscChars.length));
		assertSame(p,    item.matchPath(0, midEscBytes,    0, midEscBytes.length));
		assertSame(p,    item.matchPath(0, midEscChars,    0, midEscChars.length));
		assertSame(p,    item.matchPath(0, lastEscBytes,   0, lastEscBytes.length));
		assertSame(p,    item.matchPath(0, lastEscChars,   0, lastEscChars.length));
		assertSame(item, item.matchPath(0, noMatchBytes,   0, noMatchBytes.length));
		assertSame(item, item.matchPath(0, noMatchChars,   0, noMatchChars.length));

		// Slow path for keys that start with '\\' but don't match (line 66-71 byte, 94-99 char):
		// "\\u006fame" starts with '\\' → slow path. Decodes to "oame" ≠ "name"/"my" → return this
		byte[] noMatchEscBytes = "\\u006fame".getBytes(StandardCharsets.UTF_8);
		char[] noMatchEscChars = "\\u006fame".toCharArray();
		assertSame(item, item.matchPath(0, noMatchEscBytes, 0, noMatchEscBytes.length));
		assertSame(item, item.matchPath(0, noMatchEscChars, 0, noMatchEscChars.length));
	}
}

