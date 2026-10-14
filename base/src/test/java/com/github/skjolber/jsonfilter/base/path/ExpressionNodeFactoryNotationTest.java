package com.github.skjolber.jsonfilter.base.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

/**
 * Slash and dot notation can be mixed.
 */

public class ExpressionNodeFactoryNotationTest {

	private ExpressionNodeFactory expressionNodeFactory = new ExpressionNodeFactory();

	private String print(String expression1, FilterType type1, String expression2, FilterType type2) {
		return expressionNodeFactory.toExpressionNode(new String[] {expression1, expression2}, new FilterType[] {type1, type2}).print();
	}

	@Test
	public void shorterPathCoversLongerPath() {
		assertEquals("user\n address (PRUNE)", print("/user/address", FilterType.PRUNE, "$.user.address.zip", FilterType.ANON));
		assertEquals("user\n address (ANON)", print("$.user.address", FilterType.ANON, "/user/address/zip", FilterType.PRUNE));
	}

	@Test
	public void samePathPrunePrecedence() {
		assertEquals("a\n b (PRUNE)", print("$.a.b", FilterType.ANON, "/a/b", FilterType.PRUNE));
		assertEquals("a\n b (PRUNE)", print("/a/b", FilterType.PRUNE, "$.a.b", FilterType.ANON));
	}
}
