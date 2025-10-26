package com.github.skjolber.jsonfilter.base.path;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.ExpressionNode;
import com.github.skjolber.jsonfilter.base.path.ExpressionNodeFactory;
import com.github.skjolber.jsonfilter.base.path.PathItem;
import com.github.skjolber.jsonfilter.base.path.PathItemFactory;

public class PathItemFactoryTest extends AbstractPathTest {

	private ExpressionNodeFactory expressionNodeFactory = new ExpressionNodeFactory();
	private PathItemFactory factory = new PathItemFactory();

	private PathItem create(String[] expressions, FilterType[] types) {
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		
		return factory.create(expressionNode);
	}

	@Test
	public void test() {
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c"};
		PathItem p1 = create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(FilterType.ANON, type);
	}

	@Test
	public void test2() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b", "/a/c"};
		PathItem root = create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "c").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test3() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c", "/a/b/d"};
		PathItem root = create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test4() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertNotNull(type);

		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "d").getType();
		assertNotNull(type);
	}
	
	@Test
	public void test5() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/b/c", "/a/*/d"};
		PathItem root = create(expressions , types);

		FilterType type = root.matchPath(1, "a").matchPath(2, "x").matchPath(3, "d").getType();
		assertEquals(type, FilterType.PRUNE);

		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(type, FilterType.ANON);
	}
	
	
	@Test
	public void testConstrain() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d"};
		PathItem root = create(expressions , types);

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
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/b/c", "/d/e/f"};
		PathItem p1 = create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(FilterType.ANON, type);
		
		type = p1.matchPath(1, "d").matchPath(2, "e").matchPath(3, "f").getType();
		assertEquals(FilterType.PRUNE, type);
	}

	@Test
	public void testAnonAndPrune2() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/grandparent/parent/child1", "/no/match"};
		PathItem p1 = create(expressions , types);
		
		FilterType type = p1.matchPath(1, "grandparent").matchPath(2, "parent").matchPath(3, "child1").getType();
		assertEquals(FilterType.ANON, type);
		
		type = p1.matchPath(1, "no").matchPath(2, "match").getType();
		assertEquals(FilterType.PRUNE, type);
	}
	
	@Test
	public void testSimpleWildcard() {
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/*"};
		PathItem p1 = create(expressions , types);
		
		FilterType type = p1.matchPath(1, "a").getType();
		assertEquals(FilterType.ANON, type);
	}

	@Test
	public void testSimpleStar() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/*/c", "/a/*/d", "/a/1/e"};
		PathItem root = create(expressions , types);

		FilterType type;
		
		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").getType();
		assertEquals(FilterType.ANON, type);
		type = root.matchPath(1, "a").matchPath(2, "1").matchPath(3, "d").getType();
		assertEquals(FilterType.ANON, type);
		
		PathItem matchPath = root.matchPath(1, "a").matchPath(2, "1").matchPath(3, "e");
		assertEquals(FilterType.PRUNE, matchPath.getType());
		
		PathItem constrain = matchPath.constrain(2);
		assertSame(root.matchPath(1, "a"), constrain);
	}

	@Test
	public void testComplexStar() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/*/c/d", "/a/*/*/c"};
		PathItem root = create(expressions , types);

		FilterType type;
		
		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").matchPath(4, "d").getType();
		assertEquals(FilterType.ANON, type);
		
		type = root.matchPath(1, "a").matchPath(2, "b").matchPath(3, "c").matchPath(4, "c").getType();
		assertEquals(FilterType.PRUNE, type);
	}

}
