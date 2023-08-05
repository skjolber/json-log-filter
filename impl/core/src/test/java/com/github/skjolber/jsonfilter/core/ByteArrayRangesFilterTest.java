package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;

public class ByteArrayRangesFilterTest {

	@Test
	public void testScanning() {
		String value = "\"abcdef\"";
		String escapedValue = "\"abc\\\"def\"";
		
		assertThat(ByteArrayRangesFilter.scanBeyondQuotedValue(value.getBytes(), 1)).isEqualTo(value.length());
		assertThat(ByteArrayRangesFilter.scanBeyondQuotedValue(escapedValue.getBytes(), 1)).isEqualTo(escapedValue.length());
		
		String booleanValue = "true";
		char[] terminators = new char[] {'}', ',', ']'};
		
		for(char terminator : terminators) {
			String terminatedValue = booleanValue + terminator;
					
			assertThat(ByteArrayRangesFilter.scanUnquotedValue(terminatedValue.getBytes(), 0)).isEqualTo(terminatedValue.length() - 1);
		}
	}

	@Test
	public void testSimpleEscapeAlignment() throws IOException {
		byte[] encoded = "abcdef\\nghijkl".getBytes(StandardCharsets.UTF_8);
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that both n and slash are present or gone
			assertTrue((string.contains("n") && string.contains("\\")) || (!string.contains("n") && !string.contains("\\")));
		}
	}
	
	@Test
	public void testDoubleSlashAlignment() throws IOException {
		byte[] encoded = "abcdef\\\\ghijkl".getBytes(StandardCharsets.UTF_8);
		for(int i = 3; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that two slashes are present, or none
			assertTrue(!string.contains("\\") || string.contains("\\\\"));
		}
	}

	@Test
	public void testJsonEncodingUnicodeAlignment() throws IOException {
		String escaped = "\\uF678";
		byte[] encoded = ("abcdefghi" + escaped + "ghijkl").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);

			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				
				String stringWithoutDigits = string.substring(0, string.lastIndexOf(' '));
				for(char c : escaped.toCharArray()) {
					assertFalse(string + " ->  " + c, stringWithoutDigits.contains(c + ""));
				}
			}
		}
	}

	@Test
	public void testUTF8EncodingAlignment() throws IOException {
		String latinScriptAlphabetCharacter = new String(new byte[]{(byte) 0xc3, (byte) 0xa5}, StandardCharsets.UTF_8); //"å"; - norwegian
		String basicMultilingualPlaneCharacter = new String(new byte[]{(byte) 0xec, (byte) 0x98, (byte) 0xa4}, StandardCharsets.UTF_8); // "오"; - korean
		String otherUnicodePlanes = new String(new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x82}, StandardCharsets.UTF_8);  // "😂";  // smilie
		
		String[] unicodes = new String[] {latinScriptAlphabetCharacter, basicMultilingualPlaneCharacter, otherUnicodePlanes};

		for(String unicode : unicodes) {
			String prefix = "abcdefghi";
			String postfix = "ghijklmnopqrstuvwxyz";

			byte[] unicodeBytes = unicode.getBytes(StandardCharsets.UTF_8);
			
			String content = prefix + unicode + postfix;
			byte[] encoded = content.getBytes(StandardCharsets.UTF_8);
			
			for(int i = 0; i < unicodeBytes.length; i++) {
				ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
				filter.addMaxLength(encoded, prefix.length() + i, encoded.length, encoded.length - i);
				
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				filter.filter(encoded, 0, encoded.length, b);
				String string = b.toString();
				assertEquals(string.length(), prefix.length() + "...TRUNCATED BY XX".length());
			}

			// make sure last included character is byte 2, 3 or 4
			// to force a check which concludes that the whole unicode character
			// (i.e. all the bytes) are included
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, prefix.length() + unicodeBytes.length, encoded.length, encoded.length);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			filter.filter(encoded, 0, encoded.length, b);
			String string = b.toString();
			assertTrue(string.startsWith(prefix + unicode));
		}
	}


	@Test
	public void testUnicodeAlignmentForBorderCase() throws IOException {
		String escaped = "\\uF678";
		byte[] encoded = ("[\"" + escaped + "\"]").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				String stringWithoutDigits = string.substring(0, string.lastIndexOf(' '));
				for(char c : escaped.toCharArray()) {
					assertFalse(string + " ->  " + c, stringWithoutDigits.contains(c + ""));
				}
			}
		}
	}
	
	private String splitAt(String string, int i) throws IOException {
		byte[] encoded = string.getBytes(StandardCharsets.UTF_8);
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(encoded.length, encoded.length);
		filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
		
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		
		filter.filter(encoded, 0, encoded.length, b);
		return b.toString();
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleEscapeCharacters() throws IOException {
		String str = "abcdefghiF678g\\n\\naaa01234567890123456789";
		assertEquals("abcdefghiF678...TRUNCATED BY 28", splitAt(str, 13));
		assertEquals("abcdefghiF678g...TRUNCATED BY 27", splitAt(str, 14));
		assertEquals("abcdefghiF678g...TRUNCATED BY 27", splitAt(str, 15));
		assertEquals("abcdefghiF678g\\n...TRUNCATED BY 25", splitAt(str, 16));
		assertEquals("abcdefghiF678g\\n...TRUNCATED BY 25", splitAt(str, 17));
		assertEquals("abcdefghiF678g\\n\\n...TRUNCATED BY 23", splitAt(str, 18));
		assertEquals("abcdefghiF678g\\n\\na...TRUNCATED BY 22", splitAt(str, 19));
		assertEquals("abcdefghiF678g\\n\\naa...TRUNCATED BY 21", splitAt(str, 20));
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleInlinedUnicodeCharacters() throws IOException {
		String str = "abcdefghiF678ghijkluuuu\\\\uF678xxx01234567890123456789";
		assertEquals("abcdefghiF678ghijkluu...TRUNCATED BY 32", splitAt(str, 21));
		assertEquals("abcdefghiF678ghijkluuu...TRUNCATED BY 31", splitAt(str, 22));
		assertEquals("abcdefghiF678ghijkluuuu...TRUNCATED BY 30", splitAt(str, 23));
		assertEquals("abcdefghiF678ghijkluuuu...TRUNCATED BY 30", splitAt(str, 24));
		assertEquals("abcdefghiF678ghijkluuuu\\\\...TRUNCATED BY 28", splitAt(str, 25));
		assertEquals("abcdefghiF678ghijkluuuu\\\\u...TRUNCATED BY 27", splitAt(str, 26));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF...TRUNCATED BY 26", splitAt(str, 27));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF6...TRUNCATED BY 25", splitAt(str, 28));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF67...TRUNCATED BY 24", splitAt(str, 29));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678...TRUNCATED BY 23", splitAt(str, 30));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678x...TRUNCATED BY 22", splitAt(str, 31));
		assertEquals("abcdefghiF678ghijkluuuu\\\\uF678xx...TRUNCATED BY 21", splitAt(str, 32));
	}
	

	@Test
	public void testSurrugateAlignment() throws IOException {
		String escaped = "\uF234";
		byte[] encoded = ("abcdefghi" + escaped + "ghijkl").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
		
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				
				String stringWithoutDigits = string.substring(0, string.lastIndexOf(' '));
				for(char c : escaped.toCharArray()) {
					assertFalse(string + " ->  " + c, stringWithoutDigits.contains(c + ""));
				}
			}
		}
	}
	
	@Test
	public void testAnonymizeSubtree() throws IOException {
		String input = IOUtils.resourceToString("/input.json", StandardCharsets.UTF_8);
		String output = IOUtils.resourceToString("/anon-subtree/output.json", StandardCharsets.UTF_8);
		
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, input.length());
		ByteArrayRangesFilter.anonymizeSubtree(input.getBytes(), 0, filter);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		filter.filter(input.getBytes(), 0, input.length(), buffer);
		
		assertThat(buffer.toString()).isEqualTo(output);
	}
	
	@Test
	public void testAnonymizeSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"*****\",", "\"*****\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, input.length());
			ByteArrayRangesFilter.anonymizeSubtree(input.getBytes(), 0, filter);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			filter.filter(input.getBytes(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}
	
	@Test
	public void testPruneSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"SUBTREE REMOVED\",", "\"SUBTREE REMOVED\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, input.length());
			filter.addPrune(0, input.length() -1);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			filter.filter(input.getBytes(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}
	
}
