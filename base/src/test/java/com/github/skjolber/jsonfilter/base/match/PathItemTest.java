package com.github.skjolber.jsonfilter.base.match;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.EndPathItem;
import com.github.skjolber.jsonfilter.base.path.MultiPathItem;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.SinglePathItem;

public class PathItemTest {

	@Test
	public void test1() {
		SinglePathItem p1 = new SinglePathItem(0, "a", null);
		SinglePathItem p2 = new SinglePathItem(1, "b", p1);
		SinglePathItem p3 = new SinglePathItem(2, "c", p2);
		PathItem p4 = new EndPathItem(3, p3, FilterType.ANON);
		
		p1.setNext(p2);
		p2.setNext(p3);
		p3.setNext(p4);
		
		assertSame(p1.matchPath("a"), p2);
		
		FilterType type = p1.matchPath("a").matchPath("b").matchPath("c").getType();
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
		
		assertSame(p1.matchPath("a"), p2);
		assertSame(p2.matchPath("c"), p31);
		assertSame(p31.matchPath("f"), p4);
		
		assertNotNull(p1.matchPath("a").matchPath("c").matchPath("f").getType());
		assertNotNull(p1.matchPath("a").matchPath("d").matchPath("g").getType());
		assertNotNull(p1.matchPath("a").matchPath("e").matchPath("h").getType());
	}

}
