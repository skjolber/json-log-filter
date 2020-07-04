package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractPathJsonFilterTest {

	@Test
	public void testRegexpExpressions() {
		AbstractPathJsonFilter.validateAnonymizeExpression("/a");
		AbstractPathJsonFilter.validateAnonymizeExpression("/a/b");
		AbstractPathJsonFilter.validateAnonymizeExpression("/a/b/*");

		AbstractPathJsonFilter.validateAnonymizeExpression(".a");
		AbstractPathJsonFilter.validateAnonymizeExpression(".a.b");
		AbstractPathJsonFilter.validateAnonymizeExpression(".a.b.*");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AbstractPathJsonFilter.validateAnonymizeExpression("/a//b");
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AbstractPathJsonFilter.validateAnonymizeExpression(".a..b");
		});
		AbstractPathJsonFilter.validateAnonymizeExpression("/abc");
		AbstractPathJsonFilter.validateAnonymizeExpression(".abc");
	}
	
	@Test
	public void testAnyPrefix() {
		assertTrue(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"//a"}));
		assertFalse(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"/a"}));
		assertTrue(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"..a"}));
		assertFalse(AbstractPathJsonFilter.hasAnyPrefix(new String[] {".a"}));
	}

	@Test
	public void testSplit() {
		String[] parse1 = AbstractPathJsonFilter.parse("/a/bc");
		assertEquals(parse1[0], "a");
		assertEquals(parse1[1], "bc");
		
		String[] parse2 = AbstractPathJsonFilter.parse(".a.bc");
		assertEquals(parse2[0], "a");
		assertEquals(parse2[1], "bc");
	}

	
}
