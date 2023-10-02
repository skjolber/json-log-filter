package com.github.skjolber.jsonfilter.base.match;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.PathItemFactory;

public class PathItemFactoryTest {
	
	@Test
	public void test() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c"};
		PathItem p1 = factory.create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(FilterType.ANON, type);
	}

	@Test
	public void test2() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b", "/a/c"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "c").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test3() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c", "/a/b/d"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test4() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = factory.create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void testConstrain() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = factory.create(expressions , types);

		assertEquals(1, root.getLevel());
		assertEquals(2, root.matchPath(1, "a").getLevel());
		assertEquals(3, root.matchPath(1, "a").matchPath(2, "b").getLevel());
		assertEquals(4, root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getLevel());
		
		PathItem matchPath = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c");
		assertEquals(4, matchPath.getLevel());
		assertEquals(4, matchPath.constrain(4).getLevel());
		assertEquals(3, matchPath.constrain(3).getLevel());
		assertEquals(2, matchPath.constrain(2).getLevel());
		assertEquals(1, matchPath.constrain(1).getLevel());
		
		PathItem constrain = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").constrain(1);
		assertSame(root, constrain);

	}

	@Test
	public void testAnonAndPrune1() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/b/c", "/d/e/f"};
		PathItem p1 = factory.create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(FilterType.ANON, type);
		
		type = p1.matchPath(1, "d").matchPath(2, "e").matchPath(3, "f").getType();
		assertEquals(FilterType.PRUNE, type);
	}

	@Test
	public void testAnonAndPrune2() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/grandparent/parent/child1", "/no/match"};
		PathItem p1 = factory.create(expressions , types);
		
		FilterType type = p1.matchPath(1, "grandparent").matchPath(2, "parent").matchPath(3, "child1").getType();
		assertEquals(FilterType.ANON, type);
		
		type = p1.matchPath(1, "no").matchPath(2, "match").getType();
		assertEquals(FilterType.PRUNE, type);
	}
	
	@Test
	public void testSimpleWildcard() {
		PathItemFactory factory = new PathItemFactory();
		
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/*"};
		PathItem p1 = factory.create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").getType();
		assertEquals(FilterType.ANON, type);
	}

}
