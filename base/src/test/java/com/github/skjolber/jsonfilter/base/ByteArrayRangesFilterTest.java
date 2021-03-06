package com.github.skjolber.jsonfilter.base;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

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
	public void testSimpleEscapeAlignment() {
		byte[] encoded = "abcdef\\nghijkl".getBytes(StandardCharsets.UTF_8);
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that both n and slash are present or gone
			assertTrue((string.contains("n") && string.contains("\\")) || (!string.contains("n") && !string.contains("\\")));
		}
	}
	
	@Test
	public void testDoubleSlashAlignment() {
		byte[] encoded = "abcdef\\\\ghijkl".getBytes(StandardCharsets.UTF_8);
		for(int i = 3; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that two slashes are present, or none
			assertTrue(!string.contains("\\") || string.contains("\\\\"));
		}
	}

	@Test
	public void testJsonEncodingUnicodeAlignment() {
		String escaped = "\\uF678";
		byte[] encoded = ("abcdefghi" + escaped + "ghijkl").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				for(char c : escaped.toCharArray()) {
					assertFalse(string.contains(c + ""));
				}
			}
		}
	}

	@Test
	public void testUTF8EncodingAlignment() {
		String latinScriptAlphabetCharacter = new String(new byte[]{(byte) 0xc3, (byte) 0xa5}, StandardCharsets.UTF_8); //"å"; - norwegian
		String basicMultilingualPlaneCharacter = new String(new byte[]{(byte) 0xec, (byte) 0x98, (byte) 0xa4}, StandardCharsets.UTF_8); // "오"; - korean
		String otherUnicodePlanes = new String(new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x82}, StandardCharsets.UTF_8);  // "😂";  // smilie
		
		String[] unicodes = new String[] {latinScriptAlphabetCharacter, basicMultilingualPlaneCharacter, otherUnicodePlanes};

		for(String unicode : unicodes) {
			String prefix = "abcdefghi";
			String postfix = "ghijkl";

			byte[] unicodeBytes = unicode.getBytes(StandardCharsets.UTF_8);
			
			String content = prefix + unicode + postfix;
			byte[] encoded = content.getBytes(StandardCharsets.UTF_8);
			
			for(int i = 0; i < unicodeBytes.length; i++) {
				ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
				filter.add(prefix.length() + i, encoded.length, -10);
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				filter.filter(encoded, 0, encoded.length, b);
				String string = b.toString();
				assertEquals(string.length(), prefix.length() + "...TRUNCATED BY XX".length());
			}

			// make sure last included character is byte 2, 3 or 4
			// to force a check which concludes that the whole unicode character
			// (i.e. all the bytes) are included
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(prefix.length() + unicodeBytes.length, encoded.length, -10);
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			filter.filter(encoded, 0, encoded.length, b);
			String string = b.toString();
			assertTrue(string.startsWith(prefix + unicode));
		}
	}


	@Test
	public void testUnicodeAlignmentForBorderCase() {
		String escaped = "\\uF678";
		byte[] encoded = ("[\"" + escaped + "\"]").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
			
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				for(char c : escaped.toCharArray()) {
					assertFalse(string.contains(c + ""));
				}
			}
		}
	}

	@Test
	public void testSurrugateAlignment() {
		String escaped = "\uF234";
		byte[] encoded = ("abcdefghi" + escaped + "ghijkl").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			
			filter.filter(encoded, 0, encoded.length, b);
			
			String string = b.toString();
		
			// check that all chars are present, or none
			if(!string.contains(escaped)) {
				for(char c : escaped.toCharArray()) {
					assertFalse(string.contains(c + ""));
				}
			}
		}
	}
	
	@Test
	public void testAnonymizeSubtree() throws IOException {
		String input = IOUtils.resourceToString("/input.json", StandardCharsets.UTF_8);
		String output = IOUtils.resourceToString("/anon-subtree/output.json", StandardCharsets.UTF_8);
		
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
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
			
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
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
			
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.addPrune(0, input.length() -1);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			filter.filter(input.getBytes(), 0, input.length(), buffer);
			
			assertThat(buffer.toString()).isEqualTo(outputs[i]);
		}
	}

	@Test
	public void testSkip() {
		String endCurlyBracket = "abcde}";
		int skipSubtree = ByteArrayRangesFilter.skipSubtree(endCurlyBracket.getBytes(), 0);
		assertEquals(skipSubtree, endCurlyBracket.length() - 1);
		
		String endComma = "abcde,";
		skipSubtree = ByteArrayRangesFilter.skipSubtree(endComma.getBytes(), 0);
		assertEquals(skipSubtree, endComma.length() - 1);
		
		String endBracket = "abcde]";
		skipSubtree = ByteArrayRangesFilter.skipSubtree(endBracket.getBytes(), 0);
		assertEquals(skipSubtree, endBracket.length() - 1);
		
		String quoted = "\"abcde\"";
		skipSubtree = ByteArrayRangesFilter.skipSubtree(quoted.getBytes(), 0);
		assertEquals(skipSubtree, quoted.length());		
	}


	
}
