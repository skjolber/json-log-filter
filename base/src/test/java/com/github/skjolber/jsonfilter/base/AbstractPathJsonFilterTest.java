package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import static com.google.common.truth.Truth.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractPathJsonFilterTest {

	public class MyAbstractPathJsonFilter extends AbstractPathJsonFilter {

		public MyAbstractPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes,
				String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ByteArrayOutputStream output) {
			return false;
		}

	}
	
	@Test
	public void testConstructor() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractPathJsonFilter(1, -2, null, null, "a", "b", "c");
		});
	}

	
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
	
	@Test
	public void testStar() {
		// should intern
		assertThat(AbstractPathJsonFilter.intern("*")).isSameInstanceAs(AbstractPathJsonFilter.STAR);
		assertThat(AbstractPathJsonFilter.intern(new char[]{'*'})).isSameInstanceAs(AbstractPathJsonFilter.STAR_CHARS);
		assertThat(AbstractPathJsonFilter.intern(new byte[]{'*'})).isSameInstanceAs(AbstractPathJsonFilter.STAR_BYTES);
		
		// should not intern
		assertThat(AbstractPathJsonFilter.intern("**")).isNotSameInstanceAs(AbstractPathJsonFilter.STAR);
		assertThat(AbstractPathJsonFilter.intern(new char[]{'*', 'x'})).isNotSameInstanceAs(AbstractPathJsonFilter.STAR_CHARS);
		assertThat(AbstractPathJsonFilter.intern(new byte[]{'*', 'x'})).isNotSameInstanceAs(AbstractPathJsonFilter.STAR_BYTES);

		// should not intern
		assertThat(AbstractPathJsonFilter.intern("x")).isNotSameInstanceAs(AbstractPathJsonFilter.STAR);
		assertThat(AbstractPathJsonFilter.intern(new char[]{'x'})).isNotSameInstanceAs(AbstractPathJsonFilter.STAR_CHARS);
		assertThat(AbstractPathJsonFilter.intern(new byte[]{'x'})).isNotSameInstanceAs(AbstractPathJsonFilter.STAR_BYTES);

		// stars should match anything
		assertTrue(AbstractPathJsonFilter.matchPath("abc", AbstractPathJsonFilter.STAR));
		assertTrue(AbstractPathJsonFilter.matchPath("abc".toCharArray(), 0, 0, AbstractPathJsonFilter.STAR_CHARS));
		assertTrue(AbstractPathJsonFilter.matchPath("abc".getBytes(), 0, 0, AbstractPathJsonFilter.STAR_BYTES));
	}
	
	@Test
	public void testMatch() {
		assertTrue(AbstractPathJsonFilter.matchPath("abc", "abc"));
		assertFalse(AbstractPathJsonFilter.matchPath("abc", "def"));
		
		char[] chars = "abcdefghijk".toCharArray();
		assertTrue(AbstractPathJsonFilter.matchPath(chars, 0, 3, "abc".toCharArray()));
		assertTrue(AbstractPathJsonFilter.matchPath(chars, 1, 4, "bcd".toCharArray()));
		assertTrue(AbstractPathJsonFilter.matchPath(chars, chars.length - 3, chars.length, "ijk".toCharArray()));
		
		byte[] bytes = "abcdefghijk".getBytes();
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, 0, 3, "abc".getBytes()));
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, 1, 4, "bcd".getBytes()));
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, bytes.length - 3, bytes.length, "ijk".getBytes()));
		
	}

	
}
