package com.github.skjolber.jsonfilter.base.match;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class PathItemFactoryTest {
	
	@Test
	public void test() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c"};
		PathItem p1 = factory.create(expressions , types);
		
		FilterType type = p1.matchPath("a").matchPath("b").matchPath("c").getType();
		assertNotNull(type);
	}

	@Test
	public void test2() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b", "/a/c"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath("a").matchPath("b").getType();
		assertNotNull(type);

		type = root.matchPath("a").matchPath("c").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test3() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c", "/a/b/d"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath("a").matchPath("b").matchPath("c").getType();
		assertNotNull(type);

		type = root.matchPath("a").matchPath("b").matchPath("d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test4() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath("a").matchPath("b").matchPath("c").getType();
		assertNotNull(type);

		type = root.matchPath("a").matchPath("b").matchPath("d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void testConstrain() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = factory.create(expressions , types);

		assertEquals(0, root.getIndex());
		assertEquals(1, root.matchPath("a").getIndex());
		assertEquals(2, root.matchPath("a").matchPath("b").getIndex());
		assertEquals(3, root.matchPath("a").matchPath("b").matchPath("c").getIndex());
		
		PathItem matchPath = root.matchPath("a").matchPath("b").matchPath("c");
		assertEquals(3, matchPath.getIndex());
		assertEquals(3, matchPath.constrain(3).getIndex());
		assertEquals(2, matchPath.constrain(2).getIndex());
		assertEquals(1, matchPath.constrain(1).getIndex());
		assertEquals(0, matchPath.constrain(0).getIndex());
		
		PathItem constrain = root.matchPath("a").matchPath("b").matchPath("c").constrain(0);
		assertSame(root, constrain);

	}


}
