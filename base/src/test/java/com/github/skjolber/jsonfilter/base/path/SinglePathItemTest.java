package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
