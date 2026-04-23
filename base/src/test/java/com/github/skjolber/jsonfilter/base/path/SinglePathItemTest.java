package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.path.SinglePathItem;

public class SinglePathItemTest {

	@Test
	public void testPath() {
		
		SinglePathItem a = new SinglePathItem(1, "a", null);
		SinglePathItem b = new SinglePathItem(1, "b", a);
		
		assertEquals(a.matchPath(1, "b"), a);
		
		assertSame(a, a.matchPath(0, "x"));
	}

	@Test
	public void testBytesAndCharsReturnNext() {
		// When level matches and fieldName matches, matchPath should return next (null here)
		SinglePathItem item = new SinglePathItem(1, "a", null);
		// item.next is null, so successful match returns null
		assertNull(item.matchPath(1, "a".getBytes(StandardCharsets.UTF_8), 0, 1));
		assertNull(item.matchPath(1, "a".toCharArray(), 0, 1));
	}

	@Test
	public void testBytesAndCharsLevelMismatch() {
		// When level mismatches, matchPath should return self
		SinglePathItem item = new SinglePathItem(1, "a", null);
		assertSame(item, item.matchPath(0, "a".getBytes(StandardCharsets.UTF_8), 0, 1));
		assertSame(item, item.matchPath(0, "a".toCharArray(), 0, 1));
	}
}
