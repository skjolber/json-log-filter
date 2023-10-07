package com.github.skjolber.jsonfilter.base.match;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.EndPathItem;
import com.github.skjolber.jsonfilter.base.path.MultiPathItem;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.SinglePathItem;

public class PathItemTest extends AbstractPathTest {

	@Test
	public void test1() {
		SinglePathItem p1 = new SinglePathItem(0, "a", null);
		SinglePathItem p2 = new SinglePathItem(1, "b", p1);
		SinglePathItem p3 = new SinglePathItem(2, "c", p2);
		PathItem p4 = new EndPathItem(3, p3, FilterType.ANON);
		
		p1.setNext(p2);
		p2.setNext(p3);
		p3.setNext(p4);
		
		assertSame(p1.matchPath(0, "a"), p2);
		
		FilterType type = p1.matchPath(0, "a").matchPath(1, "b").matchPath(2, "c").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test2() {
		
		// /a/c/f
		// /a/d/g
		// /a/e/h
		
		SinglePathItem p1 = new SinglePathItem(0, "a", null);
		MultiPathItem p2 = new MultiPathItem(new String[]{"c", "d", "e"}, 1, p1);
		
		SinglePathItem p31 = new SinglePathItem(2, "f", p2);
		SinglePathItem p32 = new SinglePathItem(2, "g", p2);
		SinglePathItem p33 = new SinglePathItem(2, "h", p2);
		
		PathItem p4 = new EndPathItem(3, p31, FilterType.ANON);
		PathItem p5 = new EndPathItem(3, p32, FilterType.ANON);
		PathItem p6 = new EndPathItem(3, p33, FilterType.ANON);

		p1.setNext(p2);
		p2.setNext(p31, 0);
		p2.setNext(p32, 1);
		p2.setNext(p33, 2);
		
		p31.setNext(p4);
		p32.setNext(p5);
		p33.setNext(p6);

		// string
		assertSame(p1.matchPath(0, "a"), p2);
		assertSame(p2.matchPath(1, "c"), p31);
		assertSame(p31.matchPath(2, "f"), p4);
		
		assertNotNull(p1.matchPath(0, "a").matchPath(1, "c").matchPath(2, "f").getType());
		assertNotNull(p1.matchPath(0, "a").matchPath(1, "d").matchPath(2, "g").getType());
		assertNotNull(p1.matchPath(0, "a").matchPath(1, "e").matchPath(2, "h").getType());
		
		// chars
		assertSame(p1.matchPath(0, aChars, 0, 1), p2);
		assertSame(p2.matchPath(1, cChars, 0, 1), p31);
		assertSame(p31.matchPath(2, fChars, 0, 1), p4);
		
		assertNotNull(p1.matchPath(0, aChars, 0, 1).matchPath(1, cChars, 0, 1).matchPath(2, fChars, 0, 1).getType());
		assertNotNull(p1.matchPath(0, aChars, 0, 1).matchPath(1, dChars, 0, 1).matchPath(2, gChars, 0, 1).getType());
		assertNotNull(p1.matchPath(0, aChars, 0, 1).matchPath(1, eChars, 0, 1).matchPath(2, hChars, 0, 1).getType());

		// bytes
		assertSame(p1.matchPath(0, aBytes, 0, 1), p2);
		assertSame(p2.matchPath(1, cBytes, 0, 1), p31);
		assertSame(p31.matchPath(2, fBytes, 0, 1), p4);
		
		assertNotNull(p1.matchPath(0, aBytes, 0, 1).matchPath(1, cBytes, 0, 1).matchPath(2, fBytes, 0, 1).getType());
		assertNotNull(p1.matchPath(0, aBytes, 0, 1).matchPath(1, dBytes, 0, 1).matchPath(2, gBytes, 0, 1).getType());
		assertNotNull(p1.matchPath(0, aBytes, 0, 1).matchPath(1, eBytes, 0, 1).matchPath(2, hBytes, 0, 1).getType());
	}

}
