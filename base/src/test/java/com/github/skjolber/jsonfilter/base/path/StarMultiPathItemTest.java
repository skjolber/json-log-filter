package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertSame;

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
}
