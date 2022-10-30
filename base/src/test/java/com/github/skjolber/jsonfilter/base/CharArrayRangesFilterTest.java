package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

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
					
			assertThat(CharArrayRangesFilter.scanUnquotedValue(terminatedValue.toCharArray(), 0)).isEqualTo(terminatedValue.length() - 1);
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
		String str = "abcdefghiF678g\\n\\naaa";
		assertEquals("abcdefghiF678...TRUNCATED BY 8", splitAt(str, 13));
		assertEquals("abcdefghiF678g...TRUNCATED BY 7", splitAt(str, 14));
		assertEquals("abcdefghiF678g...TRUNCATED BY 7", splitAt(str, 15));
		assertEquals("abcdefghiF678g\\n...TRUNCATED BY 5", splitAt(str, 16));
		assertEquals("abcdefghiF678g\\n...TRUNCATED BY 5", splitAt(str, 17));
		assertEquals("abcdefghiF678g\\n\\n...TRUNCATED BY 3", splitAt(str, 18));
		assertEquals("abcdefghiF678g\\n\\na...TRUNCATED BY 2", splitAt(str, 19));
		assertEquals("abcdefghiF678g\\n\\naa...TRUNCATED BY 1", splitAt(str, 20));
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleInlinedUnicodeCharacters() {
		String str = "abcdefghiF678ghijkluuuu\\\\uF678xxx";
		assertEquals("abcdefghiF678ghijkluu...TRUNCATED BY 12", splitAt(str, 21));
		assertEquals("abcdefghiF678ghijkluuu...TRUNCATED BY 11", splitAt(str, 22));
		assertEquals("abcdefghiF678ghijkluuuu...TRUNCATED BY 10", splitAt(str, 23));
		assertEquals("abcdefghiF678ghijkluuuu...TRUNCATED BY 10", splitAt(str, 24));
		assertEquals("abcdefghiF678ghijkluuuu\\\\...TRUNCATED BY 8", splitAt(str, 25));
		assertEquals("abcdefghiF678ghijkluuuu\\\\u...TRUNCATED BY 7", splitAt(str, 26));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF...TRUNCATED BY 6", splitAt(str, 27));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF6...TRUNCATED BY 5", splitAt(str, 28));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF67...TRUNCATED BY 4", splitAt(str, 29));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678...TRUNCATED BY 3", splitAt(str, 30));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678x...TRUNCATED BY 2", splitAt(str, 31));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678xx...TRUNCATED BY 1", splitAt(str, 32));
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
	public void testAnonymizeSubtree() throws IOException {
		String input = IOUtils.resourceToString("/input.json", StandardCharsets.UTF_8);
		String output = IOUtils.resourceToString("/anon-subtree/output.json", StandardCharsets.UTF_8);
		
		CharArrayRangesFilter filter = new CharArrayRangesFilter(12, input.length());
		CharArrayRangesFilter.anonymizeSubtree(input.toCharArray(), 0, filter);
		
		StringBuilder buffer = new StringBuilder();
		filter.filter(input.toCharArray(), 0, input.length(), buffer);
		
		assertThat(buffer.toString()).isEqualTo(output);
	}
	
	@Test
	public void testAnonymizeSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"*****\",", "\"*****\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, inputs.length);
			CharArrayRangesFilter.anonymizeSubtree(input.toCharArray(), 0, filter);
			
			StringBuilder buffer = new StringBuilder();
			filter.filter(input.toCharArray(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}
	
	@Test
	public void testPruneSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"SUBTREE REMOVED\",", "\"SUBTREE REMOVED\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			CharArrayRangesFilter filter = new CharArrayRangesFilter(12, input.length());
			filter.addPrune(0, input.length() -1);
			
			StringBuilder buffer = new StringBuilder();
			filter.filter(input.toCharArray(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}

}
