package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ByteArrayRangesFilterTest {

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
			assertTrue(string, (string.contains("n") && string.contains("\\")) || (!string.contains("n") && !string.contains("\\")));
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
			assertTrue(string, !string.contains("\\") || string.contains("\\\\"));
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
					assertFalse(string + " ->  " + c, string.contains(c + ""));
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
				assertEquals(string, string.length(), prefix.length() + "...TRUNCATED BY XX".length());
			}

			// make sure last included character is byte 2, 3 or 4
			// to force a check which concludes that the whole unicode character
			// (i.e. all the bytes) are included
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12);
			filter.add(prefix.length() + unicodeBytes.length, encoded.length, -10);
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			filter.filter(encoded, 0, encoded.length, b);
			String string = b.toString();
			assertTrue(string, string.startsWith(prefix + unicode));
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
					assertFalse(string + " ->  " + c, string.contains(c + ""));
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
					assertFalse(string + " ->  " + c, string.contains(c + ""));
				}
			}
		}
	}	
}
