package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

/**
 * A wildcard which is filtered as a whole also covers its explicit siblings.
 */

public class ExpressionNodeFactoryWildcardLeafTest {

	private ExpressionNodeFactory expressionNodeFactory = new ExpressionNodeFactory();

	private String print(String expression1, FilterType type1, String expression2, FilterType type2) {
		return expressionNodeFactory.toExpressionNode(new String[] {expression1, expression2}, new FilterType[] {type1, type2}).print();
	}

	@Test
	public void wildcardLeafCoversSiblingWithChildren() {
		// /a/* matches /a/b too, so b is anonymized as a whole
		assertEquals("a\n * (ANON)\n b (ANON)", print("/a/*", FilterType.ANON, "/a/b/c", FilterType.ANON));
	}

	@Test
	public void wildcardLeafCoversSiblingLeaf() {
		assertEquals("a\n * (ANON)\n b (PRUNE)", print("/a/*", FilterType.ANON, "/a/b", FilterType.PRUNE));
		assertEquals("a\n * (PRUNE)\n b (PRUNE)", print("/a/*", FilterType.PRUNE, "/a/b", FilterType.ANON));
	}

	@Test
	public void wildcardChildLeafCoversSiblingChild() {
		// /*/password matches /user/password too, so it is pruned as a whole
		assertEquals("*\n password (PRUNE)\nuser\n password (PRUNE)", print("/*/password", FilterType.PRUNE, "/user/password/x", FilterType.ANON));
	}
}
