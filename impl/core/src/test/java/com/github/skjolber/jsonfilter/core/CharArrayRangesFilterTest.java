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

import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

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

}
