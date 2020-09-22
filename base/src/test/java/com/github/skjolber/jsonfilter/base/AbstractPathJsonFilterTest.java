package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.*;

import org.apache.commons.codec.binary.Hex;
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
	
	@Test
	public void testMatchEscaped() {
		char[] special = {'"', '\\', '/', 0x08, 0x09, 0x0C, 0x0A, 0x0D};
		
		for(char c : special) {
			String[] strings = {"abc"+c, c + "abc", "ab" + c + "c"};
			for(String string : strings) {
				char[] charArray = string.toCharArray();
				StringBuilder builder = new StringBuilder();
				AbstractJsonFilter.quoteAsString(string, builder);
				assertTrue(AbstractPathJsonFilter.matchPath(builder.toString().toCharArray(), 0, builder.length(), charArray));
			}
		}
	}

	@Test
	public void testMatchUnicodes() {
		for(int k = 0; k < Character.MIN_HIGH_SURROGATE; k++) {
			String original = toString(k);
			int[] tests = {k, k & 0xFFF, k & 0xFF, k & 0xF, 0};
			for(int i = 0; i < tests.length; i++) {
				String to = toString(tests[i]);				
				String unicode = unicode(to.charAt(0));
				
				test(original, unicode, tests[i] == k);
			}
		}

		int highSurrogates = Character.MAX_HIGH_SURROGATE - Character.MIN_HIGH_SURROGATE;
		int lowSurrogates = Character.MAX_LOW_SURROGATE - Character.MIN_LOW_SURROGATE;
		for(int i = 0; i < highSurrogates; i++) {
			for(int k = 0; k < lowSurrogates; k++) {
				
				String original = new String(new char[] {(char)(i + Character.MIN_HIGH_SURROGATE), (char)(k + Character.MIN_LOW_SURROGATE)});
				String other = new String(new char[] {(char)(Character.MAX_HIGH_SURROGATE - i), (char)(Character.MAX_LOW_SURROGATE - k)});
				
				String originalUnicode = unicode(original.charAt(0)) + unicode(original.charAt(1));
				
				String otherUnicode = unicode(other.charAt(0)) + unicode(other.charAt(1));

				String shortUnicode = unicode(original.charAt(0));

				if(!Character.isHighSurrogate(original.charAt(0))) {
					throw new RuntimeException();
				}
				if(!Character.isLowSurrogate(original.charAt(1))) {
					throw new RuntimeException();
				}
				
				test(original, originalUnicode, true);
				test(original, otherUnicode, original.equals(other));
				test(original, shortUnicode, false);
			}
		}
		
		String otherUnicodePlanes = new String(new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x82}, StandardCharsets.UTF_8);  // "😂";  // smilie
		String[] unicodes = new String[] {otherUnicodePlanes};

		for(String original : unicodes) {
			String originalUnicode = unicode(original.charAt(0)) + unicode(original.charAt(1));
			test(original, originalUnicode, true);
		}
	}

	private String toString(int k) {
		StringBuilder builder = new StringBuilder();
		builder.appendCodePoint(k);
		String raw = builder.toString();
		return raw;
	}

	public void test(String raw, String unicode, boolean result) {
		String[] expectedString = {"abc"+raw, raw + "abc", "ab" + raw + "c"};
		String[] unicodeStrings = {"abc"+unicode, unicode + "abc", "ab" + unicode + "c"};
		for(int i = 0; i < unicodeStrings.length; i++) {
			char[] unicodeChars = unicodeStrings[i].toCharArray();
			char[] expectedChars = expectedString[i].toCharArray();

			boolean charResult = AbstractPathJsonFilter.matchPath(unicodeChars, 0, unicodeChars.length, expectedChars);
			assertEquals(result, charResult, unicodeStrings[i] + " #" + i);

			byte[] unicodeBytes = unicodeStrings[i].getBytes(StandardCharsets.UTF_8);
			byte[] expectedBytes = expectedString[i].getBytes(StandardCharsets.UTF_8);

			boolean byteResult = AbstractPathJsonFilter.matchPath(unicodeBytes, 0, unicodeBytes.length, expectedBytes);
			assertEquals(result, byteResult, expectedString[i] + " -> " + new String(Hex.encodeHex(expectedBytes)) + " " + unicodeStrings[i] + " #" + i);
		}		
	}

	private String unicode(char raw) {
		StringBuilder b = new StringBuilder();
		
		b.append("\\u");
		
		b.append(AbstractJsonFilter.HC[(raw >> 12) & 0xF]); // 4
		b.append(AbstractJsonFilter.HC[(raw >> 8) & 0xF]); // 4
		b.append(AbstractJsonFilter.HC[(raw >> 4) & 0xF]); // 4
		b.append(AbstractJsonFilter.HC[raw & 0xF]); // 5
		
		return b.toString();
	}
	
}
