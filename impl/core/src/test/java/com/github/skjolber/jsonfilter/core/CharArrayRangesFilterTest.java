package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.CharArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

public class CharArrayRangesFilterTest {

	@Test
	public void testScanning() {
		String value = "\"abcdef\"";
		String escapedValue = "\"abc\\\"def\"";
		
		assertThat(CharArrayRangesFilter.scanBeyondQuotedValue(value.toCharArray(), 1)).isEqualTo(value.length());
		assertThat(CharArrayRangesFilter.scanBeyondQuotedValue(escapedValue.toCharArray(), 1)).isEqualTo(escapedValue.length());
		
		String booleanValue = "true";
		char[] terminators = new char[] {'}', ',', ']'};
		
		for(char terminator : terminators) {
			String terminatedValue = booleanValue + terminator;
					
			assertThat(CharArrayRangesFilter.scanBeyondUnquotedValue(terminatedValue.toCharArray(), 0)).isEqualTo(terminatedValue.length() - 1);
		}
		
	}

	private static final String TOP_LEVEL_STRING = "\"hello world, a long string\"";
	private static final String TOP_LEVEL_STRING_TRUNCATED = "\"hello... + 21\"";

	@Test
	public void testScanQuotedValue() {
		// the scan checks four characters per iteration, so cover every length modulo four,
		// with escaped quotes and backslashes at different positions
		String[] values = new String[] {
			"", "a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg", "abcdefgh", "abcdefghi",
			"\\\"", "a\\\"", "a\\\"b", "\\\"abcd", "abcd\\\"", "\\\"\\\"\\\"",
			"\\\\", "a\\\\", "\\\\\\\"", "abc\\\\\\\"d", "\\\\\\\\\\\\"
		};
		String[] suffixes = new String[] {"", "x", "xx", "xxx", ",\"key\":1}"};

		for (String suffix : suffixes) {
			for (String value : values) {
				String json = '"' + value + '"' + suffix;
				int closing = value.length() + 1;

				assertEquals(closing, CharArrayRangesFilter.scanQuotedValue(json.toCharArray(), 0), json);
				assertEquals(closing + 1, CharArrayRangesFilter.scanBeyondQuotedValue(json.toCharArray(), 0), json);
			}
		}
	}

	@Test
	public void testFilterNonStructural() {
		// paths cannot match, so a string is only truncated, and surrounding whitespace optionally removed
		for(String input : new String[] {"\"hello\"", " \"hello\" \n", "\"a\\\"b\\\\\"", TOP_LEVEL_STRING}) {
			assertEquals(input, filterNonStructural(input, Integer.MAX_VALUE, false, null));
			assertEquals(input, filterNonStructural(input, 100, false, null));
		}
		assertEquals("\"hello\"", filterNonStructural(" \"hello\" \n", Integer.MAX_VALUE, true, null));
		assertEquals("\"hello\"", filterNonStructural("\r\n\t\"hello\"", Integer.MAX_VALUE, true, null));
		assertEquals("\"a\\\"b\\\\\"", filterNonStructural(" \"a\\\"b\\\\\" ", Integer.MAX_VALUE, true, null));

		assertEquals(TOP_LEVEL_STRING_TRUNCATED, filterNonStructural(TOP_LEVEL_STRING, 5, false, null));
		assertEquals(" " + TOP_LEVEL_STRING_TRUNCATED + "\n", filterNonStructural(" " + TOP_LEVEL_STRING + "\n", 5, false, null));
		assertEquals(TOP_LEVEL_STRING_TRUNCATED, filterNonStructural(" " + TOP_LEVEL_STRING + "\n", 5, true, null));

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		assertEquals(TOP_LEVEL_STRING_TRUNCATED, filterNonStructural(TOP_LEVEL_STRING, 5, false, metrics));
		assertEquals(1, metrics.getMaxStringLength());

		// numbers and literals are never truncated
		for(String input : new String[] {"123456789", "-1.5e3", "true", "false", "null"}) {
			assertEquals(input, filterNonStructural(input, 1, false, null));
			assertEquals(" " + input + "\n", filterNonStructural(" " + input + "\n", 1, false, null));
			assertEquals(input, filterNonStructural(" \r\n" + input + "\t ", 1, true, null));
		}
	}

	@Test
	public void testFilterNonStructuralSlice() {
		// a colon after the slice must not make the string look like a key
		String input = "x" + TOP_LEVEL_STRING + ":y";
		assertEquals(TOP_LEVEL_STRING_TRUNCATED, filterNonStructural(input, 1, TOP_LEVEL_STRING.length(), 5, false, null));

		// whitespace is removed within the slice only
		input = "x " + TOP_LEVEL_STRING + " :y";
		assertEquals(TOP_LEVEL_STRING_TRUNCATED, filterNonStructural(input, 1, TOP_LEVEL_STRING.length() + 2, 5, true, null));
		assertEquals(" " + TOP_LEVEL_STRING + " ", filterNonStructural(input, 1, TOP_LEVEL_STRING.length() + 2, Integer.MAX_VALUE, false, null));

		// a number ends at the end of the slice
		input = "[12345]";
		assertEquals("123", filterNonStructural(input, 1, 3, Integer.MAX_VALUE, false, null));
		input = "x 123 :y";
		assertEquals("123", filterNonStructural(input, 1, 5, Integer.MAX_VALUE, true, null));
		assertEquals(" 123 ", filterNonStructural(input, 1, 5, Integer.MAX_VALUE, false, null));
	}

	@Test
	public void testAddNonStructural() {
		// no ranges for a string which is kept as-is
		assertEquals(0, CharArrayRangesFilter.addNonStructural(TOP_LEVEL_STRING.toCharArray(), 0, TOP_LEVEL_STRING.length(), Integer.MAX_VALUE, false, new CharArrayRangesFilter(-1, TOP_LEVEL_STRING.length())).getFilterIndex());

		// one range per whitespace run, one for truncation
		String input = " " + TOP_LEVEL_STRING + "\n";
		assertEquals(9, CharArrayRangesFilter.addNonStructural(input.toCharArray(), 0, input.length(), 5, true, new CharArrayRangesFilter(-1, input.length())).getFilterIndex());

		// one range per whitespace run for a number
		input = " 123\n";
		assertEquals(6, CharArrayRangesFilter.addNonStructural(input.toCharArray(), 0, input.length(), 1, true, new CharArrayRangesFilter(-1, input.length())).getFilterIndex());
		assertEquals(0, CharArrayRangesFilter.addNonStructural(input.toCharArray(), 0, input.length(), 1, false, new CharArrayRangesFilter(-1, input.length())).getFilterIndex());
	}

	private static String filterNonStructural(String input, int maxStringLength, boolean removeWhitespace, JsonFilterMetrics metrics) {
		return filterNonStructural(input, 0, input.length(), maxStringLength, removeWhitespace, metrics);
	}

	private static String filterNonStructural(String input, int offset, int length, int maxStringLength, boolean removeWhitespace, JsonFilterMetrics metrics) {
		StringBuilder output = new StringBuilder();
		CharArrayRangesFilter.filterNonStructural(input.toCharArray(), offset, length, output, metrics, maxStringLength, removeWhitespace, "\"[removed]\"".toCharArray(), "\"*\"".toCharArray(), "... + ".toCharArray());
		return output.toString();
	}
	
	@Test
	public void testSimpleEscapeAlignment() {
		char[] encoded = "abcdef\\nghijkl".toCharArray();
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, 10);
			
			StringBuilder b = new StringBuilder();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that both n and slash are present or gone
			assertTrue(string, (string.contains("n") && string.contains("\\")) || (!string.contains("n") && !string.contains("\\")));
		}
	}
	
	@Test
	public void testDoubleSlashAlignment() {
		char[] encoded = "abcdef\\\\ghijkl".toCharArray();
		for(int i = 3; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, 10);
			
			StringBuilder b = new StringBuilder();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that two slashes are present, or none
			assertTrue(string, !string.contains("\\") || string.contains("\\\\"));
		}
	}

	@Test
	public void testUnicodeAlignment() {
		String escaped = "\\uF678";
		char[] encoded = ("abcdefghi" + escaped + "ghijkl").toCharArray();
		
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			StringBuilder b = new StringBuilder();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			// check that all escape chars are present, or none
			if(!string.contains(escaped)) {
				String stringWithoutDigits = string.substring(0, string.lastIndexOf(' '));
				for(char c : escaped.toCharArray()) {
					assertFalse(string + " ->  " + c, stringWithoutDigits.contains(c + ""));
				}
			}
		}
	}

	@Test
	public void testNoUnicodeAlignment() {
		String[] strings = new String[] {
			// regular cases
			"abcdefghiF678ghijkluuuuuF678xxx",
			// corner cases
			"ABuCDEF", 
			"ABCDEF",
		};
		for(String string : strings) {
			char[] encoded = string.toCharArray();
			
			for(int i = 2; i < encoded.length; i++) {
	
				String b = splitAt(string, i);

				int index = i;

				CharSequence subSequence = b.subSequence(0,  index);

				assertTrue(b + " vs " + subSequence, string.contains(subSequence));
			} 
		}
	}

	private String splitAt(String string, int i) {
		char[] encoded = string.toCharArray();
		CharArrayRangesFilter filter = new CharArrayRangesFilter(encoded.length, encoded.length);
		filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
		
		StringBuilder b = new StringBuilder();
		
		filter.filter(encoded, 0, encoded.length, b);
		return b.toString();
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleEscapeCharacters() {
		String str = "abcdefghiF678g\\n\\naaa01234567890123456789";
		assertEquals("abcdefghiF678... + 28", splitAt(str, 13));
		assertEquals("abcdefghiF678g... + 27", splitAt(str, 14));
		assertEquals("abcdefghiF678g... + 27", splitAt(str, 15));
		assertEquals("abcdefghiF678g\\n... + 25", splitAt(str, 16));
		assertEquals("abcdefghiF678g\\n... + 25", splitAt(str, 17));
		assertEquals("abcdefghiF678g\\n\\n... + 23", splitAt(str, 18));
		assertEquals("abcdefghiF678g\\n\\na... + 22", splitAt(str, 19));
		assertEquals("abcdefghiF678g\\n\\naa... + 21", splitAt(str, 20));
	}

	@Test
	public void testNoUnicodeAlignmentForBackspaceAndFormFeedEscapes() {
		// b and f are hex digits, but \b and \f are single character escapes
		String str = "abcdefghiF678g\\b\\faaa01234567890123456789";
		assertEquals("abcdefghiF678... + 28", splitAt(str, 13));
		assertEquals("abcdefghiF678g... + 27", splitAt(str, 14));
		assertEquals("abcdefghiF678g... + 27", splitAt(str, 15));
		assertEquals("abcdefghiF678g\\b... + 25", splitAt(str, 16));
		assertEquals("abcdefghiF678g\\b... + 25", splitAt(str, 17));
		assertEquals("abcdefghiF678g\\b\\f... + 23", splitAt(str, 18));
		assertEquals("abcdefghiF678g\\b\\fa... + 22", splitAt(str, 19));
		assertEquals("abcdefghiF678g\\b\\faa... + 21", splitAt(str, 20));
	}

	@Test
	public void testStringAlignmentForBackspaceAndFormFeedEscapes() {
		// a cut at the b or f of the escape moves to the backslash, a cut after the escape stays
		for(String string : new String[] {"\"aaaa\\bccc\"", "\"aaaa\\fccc\"", "\"\\\\u\\bccc\""}) {
			int index = string.lastIndexOf('\\'); // backslash of the \b or \f escape

			assertEquals(index, CharArrayRangesFilter.getStringAlignment(string.toCharArray(), index + 1));
			assertEquals(index + 2, CharArrayRangesFilter.getStringAlignment(string.toCharArray(), index + 2));
		}

		// an escaped backslash followed by a plain b or f: a cut before the letter stays
		for(String string : new String[] {"\"aaa\\\\bccc\"", "\"aaa\\\\fccc\""}) {
			assertEquals(6, CharArrayRangesFilter.getStringAlignment(string.toCharArray(), 6));
		}
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleInlinedUnicodeCharacters() {
		String str = "abcdefghiF678ghijkluuuu\\\\uF678xxx01234567890123456789";
		assertEquals("abcdefghiF678ghijkluu... + 32", splitAt(str, 21));
		assertEquals("abcdefghiF678ghijkluuu... + 31", splitAt(str, 22));
		assertEquals("abcdefghiF678ghijkluuuu... + 30", splitAt(str, 23));
		assertEquals("abcdefghiF678ghijkluuuu... + 30", splitAt(str, 24));
		assertEquals("abcdefghiF678ghijkluuuu\\\\... + 28", splitAt(str, 25));
		assertEquals("abcdefghiF678ghijkluuuu\\\\u... + 27", splitAt(str, 26));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF... + 26", splitAt(str, 27));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF6... + 25", splitAt(str, 28));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF67... + 24", splitAt(str, 29));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678... + 23", splitAt(str, 30));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678x... + 22", splitAt(str, 31));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678xx... + 21", splitAt(str, 32));
	}
	
	@Test
	public void testUnicodeAlignmentForBorderCase() {
		String escaped = "\\uF678";
		char[] encoded = ("[\"" + escaped + "\"]").toCharArray();
		
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, 10);

			StringBuilder b = new StringBuilder();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that all escape chars are present, or none
			if(!string.contains(escaped)) {
				for(char c : escaped.toCharArray()) {
					assertFalse(string + " ->  " + c, string.contains(c + ""));
				}
			}
		}
	}

	@Test
	public void testSurrogateAlignment() {
		String escaped = "\uD800\uDF48B";
		char[] encoded = ("abcdefghi" + escaped + "ghijkl").toCharArray();
		
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, 10);
			
			StringBuilder b = new StringBuilder();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
		
			// check that all valid code points
			
			for(int k = 0; k < string.length(); k++) {
				if(Character.isHighSurrogate(string.charAt(k))) {
					k++;
					if(!Character.isLowSurrogate(string.charAt(k))) {
						fail();
					}
				}
			}
		}
	}
	
	@Test
	public void testEscapedSurrogatePairAlignment() {
		// a cut within an escaped surrogate pair (here U+1F600) removes the whole pair
		for(String pair : new String[] {"\\uD83D\\uDE00", "\\ud83d\\ude00"}) {
			String str = "ab" + pair + "cdefghijklmnopqrstuvwxyz0123";
			for(int i = 3; i < 14; i++) {
				assertEquals("ab... + 40", splitAt(str, i));
			}
			assertEquals("ab" + pair + "... + 28", splitAt(str, 14));
		}

		// a cut within the second of two consecutive pairs keeps the first pair
		String pair = "\\uD83D\\uDE00";
		String str = pair + pair + "cdefghijklmnopqrstuvwxyz0123";
		for(int i = 12; i < 24; i++) {
			assertEquals(pair + "... + 40", splitAt(str, i));
		}
		assertEquals(pair + pair + "... + 28", splitAt(str, 24));

		// an escaped backslash followed by the plain text uD83D, which is not a high surrogate
		assertEquals("a\\\\uD83D... + 29", splitAt("a\\\\uD83Dbcdefghijklmnopqrstuvwxyz0123", 8));
		// an escaped backslash followed by an escaped surrogate pair
		assertEquals("a\\\\... + 41", splitAt("a\\\\" + pair + "bcdefghijklmnopqrstuvwxyz0123", 9));
	}

	@Test
	public void testStringAlignmentForEscapedSurrogatePair() {
		// quote, two chars, then the pair at index 3 to 14: a cut within the pair moves to its start
		char[] chars = "\"ab\\uD83D\\uDE00cd\"".toCharArray();
		assertEquals(3, CharArrayRangesFilter.getStringAlignment(chars, 3));
		for(int start = 4; start < 15; start++) {
			assertEquals(3, CharArrayRangesFilter.getStringAlignment(chars, start), Integer.toString(start));
		}
		assertEquals(15, CharArrayRangesFilter.getStringAlignment(chars, 15));

		// a cut right after an escaped high surrogate (D800 to DBFF) moves to its start, whatever follows
		for(String escape : new String[] {"\\uD800", "\\uD83D", "\\uDBFF", "\\udbff"}) {
			String string = "\"ab" + escape + "\\u00e9cd\"";
			assertEquals(3, CharArrayRangesFilter.getStringAlignment(string.toCharArray(), 9), escape);
		}

		// a cut right after any other escape stays
		for(String escape : new String[] {"\\uD7FF", "\\uDC00", "\\uE000", "\\u0800"}) {
			String string = "\"ab" + escape + "\\u00e9cd\"";
			assertEquals(9, CharArrayRangesFilter.getStringAlignment(string.toCharArray(), 9), escape);
		}

		// a cut after an escaped backslash followed by the plain text uD83D stays
		assertEquals(9, CharArrayRangesFilter.getStringAlignment("\"a\\\\uD83Dxy\"".toCharArray(), 9));
	}

	@Test
	public void testIsHighSurrogateSecondHex() {
		for(char c = 0; c < 256; c++) {
			boolean expected = c == '8' || c == '9' || c == 'A' || c == 'B' || c == 'a' || c == 'b';
			assertEquals(expected, CharArrayRangesFilter.isHighSurrogateSecondHex(c), Integer.toString(c));
		}
		assertFalse(CharArrayRangesFilter.isHighSurrogateSecondHex((char)0xD83D));
		assertFalse(CharArrayRangesFilter.isHighSurrogateSecondHex(Character.MAX_VALUE));
	}

	@Test
	public void testAnonymizeSubtree() throws IOException {
		String input = IOUtils.resourceToString("/input.json", StandardCharsets.UTF_8);
		String output = IOUtils.resourceToString("/anon-subtree/output.json", StandardCharsets.UTF_8);
		
		CharArrayRangesFilter filter = new CharArrayRangesFilter(12, input.length());
		CharArrayRangesFilter.anonymizeObjectOrArray(input.toCharArray(), 0, filter);
		
		StringBuilder buffer = new StringBuilder();
		filter.filter(input.toCharArray(), 0, input.length(), buffer);
		
		assertThat(buffer.toString()).isEqualTo(output);
	}

	@Test
	public void testPruneSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"[removed]\",", "\"[removed]\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, input.length());
			filter.addPrune(0, input.length() -1);
			
			StringBuilder buffer = new StringBuilder();
			filter.filter(input.toCharArray(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}

	@Test
	public void testOutputMetricsIncludeClosedStructure() {
		// cut within an array within an object: the closing brackets added by the size filter are output too
		String input = "{\"a\":[1,2,3]}";
		char[] chars = input.toCharArray();

		CharArrayRangesSizeFilter filter = new CharArrayRangesSizeFilter(12, chars.length, "\"[removed]\"".toCharArray(), "\"*\"".toCharArray(), "...".toCharArray());
		filter.addDelete(7, chars.length);
		filter.setLevel(2);
		filter.getSquareBrackets()[1] = true;

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		StringBuilder buffer = new StringBuilder();
		filter.filter(chars, 0, chars.length, buffer, metrics);

		assertThat(buffer.toString()).isEqualTo("{\"a\":[1]}");
		assertEquals(buffer.length(), metrics.getOutputSize());
		assertEquals(chars.length, metrics.getInputSize());
	}

	@Test
	public void testSkipArray() {
		// Simple array [1,2,3] - offset starts BEFORE the opening bracket 
		char[] json = "[1,2,3]x".toCharArray();
		// offset 0 is the '[', skipArray increments immediately, scanning from 1
		int end = CharArrayRangesFilter.skipArray(json, 0);
		assertEquals(7, end);

		// Nested arrays [[1,2],[3,4]]
		char[] nested = "[[1,2],[3,4]]x".toCharArray();
		end = CharArrayRangesFilter.skipArray(nested, 0);
		assertEquals(13, end);
	}

	@Test
	public void testIsEmpty() {
		CharArrayRangesFilter filter = new CharArrayRangesFilter(3, 10);
		assertTrue(filter.isEmpty());

		filter.addAnon(2, 5);
		assertFalse(filter.isEmpty());
	}
}
