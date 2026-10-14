package com.github.skjolber.jsonfilter.base;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unsupported syntax, which would silently never match, is rejected.
 */

public class PathExpressionValidationTest {

	private static void assertRejected(String expression) {
		assertThrows(IllegalArgumentException.class, () -> AbstractPathJsonFilter.validateAnonymizeExpression(expression), expression);
		assertThrows(IllegalArgumentException.class, () -> AbstractPathJsonFilter.validatePruneExpression(expression), expression);
	}

	private static void assertAccepted(String expression) {
		AbstractPathJsonFilter.validateAnonymizeExpression(expression);
		AbstractPathJsonFilter.validatePruneExpression(expression);
	}

	@Test
	public void bracketNotationIsRejected() {
		// arrays are transparent, so $.items.card matches card within an array
		assertRejected("$.items[*].card");
		assertRejected("$.items[0].card");
		assertRejected("/items[0]/card");
	}

	@Test
	public void whitespaceAroundSegmentIsRejected() {
		assertRejected("$..card ");
		assertRejected(" $.a");
		assertRejected("$. a.b");
		assertRejected("/a /b");
		assertRejected("// card");
	}

	@Test
	public void partialWildcardIsRejected() {
		assertRejected("$.a*");
		assertRejected("/a/b*c");
		assertRejected("$..a*");
	}

	@Test
	public void supportedExpressionsAreAccepted() {
		assertAccepted("$.a.b");
		assertAccepted("/a/b");
		assertAccepted("$.a.*.b");
		assertAccepted("/a/*");
		assertAccepted("$..a");
		assertAccepted("//a");
		// whitespace within a segment is part of the key
		assertAccepted("$.first name");
		assertAccepted("/first name/x");
	}
}
