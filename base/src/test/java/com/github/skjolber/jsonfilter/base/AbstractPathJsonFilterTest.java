package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

public class AbstractPathJsonFilterTest {

	public class MyAbstractPathJsonFilter extends AbstractPathJsonFilter {

		public MyAbstractPathJsonFilter(int maxStringLength, int maxPathMatches, String[] anonymizes, String[] prunes,
				String pruneMessage, String anonymizeMessage, String truncateMessage) {
			super(maxStringLength, -1, maxPathMatches, anonymizes, prunes, pruneMessage, anonymizeMessage, truncateMessage);
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output) {
			return false;
		}

		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output) {
			return false;
		}

		@Override
		public boolean process(char[] chars, int offset, int length, StringBuilder output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}


		@Override
		public boolean process(byte[] chars, int offset, int length, ResizableByteArrayOutputStream output,
				JsonFilterMetrics filterMetrics) {
			return false;
		}
	}
	
	@Test
	public void testConstructor() {
		MyAbstractPathJsonFilter filter = new MyAbstractPathJsonFilter(1, 5, new String[] {"/a/b"}, new String[] {"/c/d"}, "a", "b", "c");
		assertThat(filter.getAnonymizes()).asList().containsExactly("/a/b");
		assertThat(filter.getPrunes()).asList().containsExactly("/c/d");
		
		assertThat(filter.getMaxPathMatches()).isEqualTo(5);
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new MyAbstractPathJsonFilter(1, -2, null, null, "a", "b", "c");
		});
		
		MyAbstractPathJsonFilter emptyFilter= new MyAbstractPathJsonFilter(1, 5, null, null, "a", "b", "c");
		assertThat(emptyFilter.getAnonymizes()).asList().isEmpty();
		assertThat(emptyFilter.getPrunes()).asList().isEmpty();
		
	}

	
	@Test
	public void testRegexpExpressions() {
		Consumer<String> p = AbstractPathJsonFilter::validateAnonymizeExpression;
		Consumer<String> a = AbstractPathJsonFilter::validatePruneExpression;
		
		for(Consumer<String> c : Arrays.asList(p, a)) { 
			c.accept("/a");
			c.accept("/a/b");
			c.accept("/a/b/*");
	
			c.accept("$.a");
			c.accept("$.a.b");
			c.accept("$.a.b.*");
	
			Assertions.assertThrows(IllegalArgumentException.class, () -> {
				c.accept("aa");
			});
			
			Assertions.assertThrows(IllegalArgumentException.class, () -> {
				c.accept("bb");
			});
			Assertions.assertThrows(IllegalArgumentException.class, () -> {
				c.accept("/a//b");
			});
			
			Assertions.assertThrows(IllegalArgumentException.class, () -> {
				c.accept("$.a..b");
			});
			c.accept("/abc");
			c.accept("$.abc");
			
			c.accept("//abc");
			c.accept("$..abc");
		}
		
	}
	
	@Test
	public void testAnyPrefix() {
		assertTrue(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"//a"}));
		assertFalse(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"/a"}));
		assertTrue(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"$..a"}));
		assertFalse(AbstractPathJsonFilter.hasAnyPrefix(new String[] {"$.a"}));
	}

	@Test
	public void testSplit() {
		String[] parse1 = AbstractPathJsonFilter.parse("/a/bc");
		assertNull(parse1[0]);
		assertEquals(parse1[1], "a");
		assertEquals(parse1[2], "bc");
		
		String[] parse2 = AbstractPathJsonFilter.parse(".a.bc");
		assertNull(parse2[0]);
		assertEquals(parse2[1], "a");
		assertEquals(parse2[2], "bc");
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
	}
	
	@Test
	public void testMatch() {
		assertTrue(AbstractPathJsonFilter.matchPath("abc", "abc"));
		assertFalse(AbstractPathJsonFilter.matchPath("abc", "def"));
		
		char[] chars = "abcdefghijk".toCharArray();
		assertTrue(AbstractPathJsonFilter.matchPath(chars, 0, 3, "abc".toCharArray()));
		assertTrue(AbstractPathJsonFilter.matchPath(chars, 1, 4, "bcd".toCharArray()));
		assertTrue(AbstractPathJsonFilter.matchPath(chars, chars.length - 3, chars.length, "ijk".toCharArray()));
		assertFalse(AbstractPathJsonFilter.matchPath(chars, chars.length - 2, chars.length, "ijk".toCharArray()));
		assertFalse(AbstractPathJsonFilter.matchPath(chars, 0, 2, "abc".toCharArray()));
		assertFalse(AbstractPathJsonFilter.matchPath(chars, 1, 5, "abc".toCharArray()));
		
		byte[] bytes = "abcdefghijk".getBytes();
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, 0, 3, "abc".getBytes()));
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, 1, 4, "bcd".getBytes()));
		assertTrue(AbstractPathJsonFilter.matchPath(bytes, bytes.length - 3, bytes.length, "ijk".getBytes()));
		assertFalse(AbstractPathJsonFilter.matchPath(bytes, bytes.length - 2, bytes.length, "ijk".getBytes()));
		assertFalse(AbstractPathJsonFilter.matchPath(bytes, 0, 2, "abc".getBytes()));
		assertFalse(AbstractPathJsonFilter.matchPath(bytes, 1, 5, "abc".getBytes()));
	}
	
	@Test
	public void testMatchTooShort() {
		// looks long enough, is not
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0041".toCharArray(), 0, 6, "AB".toCharArray()));
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0041".getBytes(), 0, 6, "AB".getBytes()));
	}

	@Test
	public void testMatchTooLong() {
		// is too long
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0041\\u0042\\u0043".toCharArray(), 0, 18, "AB".toCharArray()));
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0041\\u0042\\u0043".getBytes(), 0, 18, "AB".getBytes()));
	}

	@Test
	public void testMatchEscaped() {
		char[] special = {'"', '\\', '/', 0x08, 0x09, 0x0C, 0x0A, 0x0D};
		
		// positive
		for(char c : special) {
			String[] cs = {"abc"+c, c + "abc", "ab" + c + "c"};

			for(char d : special) {
				String[] ds = {"abc"+d, d + "abc", "ab" + d + "c"};

				for(int i = 0; i < ds.length; i++) {
					StringBuilder builder = new StringBuilder();
					AbstractJsonFilter.quoteAsString(ds[i], builder);
					
					assertEquals(c == d, AbstractPathJsonFilter.matchPath(builder.toString().getBytes(), 0, builder.length(), cs[i].getBytes()));
					assertEquals(c == d, AbstractPathJsonFilter.matchPath(builder.toString().toCharArray(), 0, builder.length(), cs[i].toCharArray()));
				}
			}
		}
	}
	
	@Test
	public void testEscapes() {
		assertTrue(AbstractPathJsonFilter.isEscape('/', '/'));
		assertFalse(AbstractPathJsonFilter.isEscape('/', ' '));
		assertFalse(AbstractPathJsonFilter.isEscape('e', 'e'));
	}
	
	@Test 
	public void testEscapesWithDoNotMatch() {
		// ascii - 1 byte
		assertFalse(AbstractPathJsonFilter.matchPath("\\uF041\\u0042".getBytes(), 0, 18, "AB".getBytes()));
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0F41\\u0042".getBytes(), 0, 18, "AB".getBytes()));
		assertFalse(AbstractPathJsonFilter.matchPath("\\uFF41\\u0042".getBytes(), 0, 18, "AB".getBytes()));
		
		// 2 bytes
		assertTrue(AbstractPathJsonFilter.matchPath("\\u00E5\\u0042".getBytes(), 0, 12, "\u00E5B".getBytes())); // å
		assertFalse(AbstractPathJsonFilter.matchPath("\\uF0E5\\u0042".getBytes(), 0, 12, "\u00E5B".getBytes())); // å
		assertFalse(AbstractPathJsonFilter.matchPath("\\u0FE5\\u0042".getBytes(), 0, 12, "\u00E5B".getBytes())); // å
		assertFalse(AbstractPathJsonFilter.matchPath("\\u00F5\\u0042".getBytes(), 0, 12, "\u00E5B".getBytes())); // å
		assertFalse(AbstractPathJsonFilter.matchPath("\\u00EF\\u0042".getBytes(), 0, 12, "\u00E5B".getBytes())); // å

		// 3 bytes
		assertTrue(AbstractPathJsonFilter.matchPath("\\u20AC\\u0042".getBytes(), 0, 12, "\u20ACB".getBytes())); // €
		assertFalse(AbstractPathJsonFilter.matchPath("\\uF0AC\\u0042".getBytes(), 0, 12, "\u20ACB".getBytes())); // €
		assertFalse(AbstractPathJsonFilter.matchPath("\\u2FAC\\u0042".getBytes(), 0, 12, "\u20ACB".getBytes())); // €
		assertFalse(AbstractPathJsonFilter.matchPath("\\u20FC\\u0042".getBytes(), 0, 12, "\u20ACB".getBytes())); // €
		assertFalse(AbstractPathJsonFilter.matchPath("\\u20AF\\u0042".getBytes(), 0, 12, "\u20ACB".getBytes())); // €
		
		// 4 bytes - two chars
		assertTrue(AbstractPathJsonFilter.matchPath("\\uD800\\uDF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uF800\\uDF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uDF00\\uDF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD8F0\\uDF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD80F\\uDF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈

		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800\\uFF48\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800\\uD048\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800\\uDFF8\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800\\uDF4F\\u0042".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		
		// 4 bytes, however escape runs out
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800B".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		
		// 4 bytes, however unexpected escape
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800\\t".getBytes(), 0, 18, "\uD800\uDF48B".getBytes())); // 𐍈
		
		// 4 bytes, however unexpectedly end of string
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800".getBytes(), 0, 6, "\uD800\uDF48B".getBytes())); // 𐍈
		
		// invalid UTF-8 length encoding
		byte[] bytes = "\uD800\uDF48B".getBytes();
		bytes[0] = (byte) 0xf8;
		assertFalse(AbstractPathJsonFilter.matchPath("\\uD800".getBytes(), 0, 6, bytes)); // 𐍈
		
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
