package com.github.skjolber.jsonfilter.base.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.path.ExpressionNode;
import com.github.skjolber.jsonfilter.base.path.ExpressionNodeFactory;

public class ExpressionNodeFactoryTest {

	private ExpressionNodeFactory expressionNodeFactory = new ExpressionNodeFactory();

	@Test
	public void test1() {
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c"};
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		
		assertEquals("a\n b\n  c (ANON)", expressionNode.print());
	}

	@Test
	public void testWildcard() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/*/c/d", "/a/*/*/c"};

		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		
		assertEquals(
				  "a\n"
				+ " *\n"
				+ "  c\n"
				+ "   d (ANON)\n"
				+ "   c (PRUNE)\n"
				+ "  *\n"
				+ "   c (PRUNE)", expressionNode.print());
	}

	@Test
	public void testWildcard2() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/b/c/d/e", "/a/*/*/*/f"};

		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
	}
	
	@Test
	public void testWildcard3() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/*/*/*/*/e", "/a/*/*/*/f"};

		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
	}

	@Test
	public void testWildcard4() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE, FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c/d/e", "/a/*/*/*/f", "/a/*/*/*/g"};

		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
	}

}
