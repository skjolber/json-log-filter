package com.github.skjolber.jsonfilter.base.match;

import static org.junit.jupiter.api.Assertions.assertSame;

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
}
