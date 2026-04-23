package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

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
	
	@Test
	public void testWildcard5() {
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE, FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c/d/e/i", "/a/*/*/*/e/i", "/a/*/*/*/g/i"};

		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
	}

	@Test
	public void testDuplicateExpressions() {
		// Duplicate expressions: /a/b/c appears twice → filter() should keep only one via equals check
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/a/b/c", "/a/b/c"};
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
		// The map gets overwritten by the last type (PRUNE), and only one copy kept by filter
		// Just verify the tree was built without error and has the expected structure
		String printed = expressionNode.print();
		assertTrue(printed.contains("a"));
		assertTrue(printed.contains("b"));
		assertTrue(printed.contains("c"));
	}

	@Test
	public void testDotNotation() {
		// Test parse() with $. prefix (dot notation)
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"$.a.b.c"};
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
	}

	@Test
	public void testListVariant() {
		// Test toExpressionNode(List, List) variant
		FilterType[] types = new FilterType[] {FilterType.ANON};
		String[] expressions = new String[]{"/a/b/c"};
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(
			java.util.Arrays.asList(expressions),
			java.util.Arrays.asList(types));
		assertNotNull(expressionNode);
	}

	@Test
	public void testExpressionNodeGetSet() {
		// Test ExpressionNode.setChildren() and getIndex()
		ExpressionNode node = new ExpressionNode();
		node.setIndex(3);
		assertEquals(3, node.getIndex());

		ExpressionNode child1 = new ExpressionNode();
		child1.setPath("a");
		ExpressionNode child2 = new ExpressionNode();
		child2.setPath("b");

		node.setChildren(java.util.Arrays.asList(child1, child2));
		assertEquals(2, node.getChildren().size());
	}

	@Test
	public void testPrefixPathBreaksLoop() {
		// ExpressionNodeFactory line 36: break when current.hasFilterType()
		// Occurs when a prefix path (/one) is followed by an extension (/one/two):
		//   After adding /one, its node has a filter type set.
		//   When processing /one/two, at k=1, one_node.hasFilterType() → break (line 36).
		FilterType[] types = new FilterType[] {FilterType.ANON, FilterType.PRUNE};
		String[] expressions = new String[]{"/one", "/one/two"};
		ExpressionNode expressionNode = expressionNodeFactory.toExpressionNode(expressions, types);
		assertNotNull(expressionNode);
		// The 'one' node should have ANON filter type (set first) and no 'two' child
		ExpressionNode oneNode = expressionNode.find("one");
		assertNotNull(oneNode);
		assertTrue(oneNode.hasFilterType());
	}

}
