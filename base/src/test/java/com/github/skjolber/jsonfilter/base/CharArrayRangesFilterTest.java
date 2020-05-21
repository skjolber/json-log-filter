package com.github.skjolber.jsonfilter.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class CharArrayRangesFilterTest {

	@Test
	public void testSimpleEscapeAlignment() {
		char[] encoded = "abcdef\\nghijkl".toCharArray();
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
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

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
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

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			StringBuilder b = new StringBuilder();
			
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
	public void testUnicodeAlignmentForBorderCase() {
		String escaped = "\\uF678";
		char[] encoded = ("[\"" + escaped + "\"]").toCharArray();
		
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			StringBuilder b = new StringBuilder();
			
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
		char[] encoded = ("abcdefghi" + escaped + "ghijkl").toCharArray();
		
		for(int i = 2; i < encoded.length; i++) {

			CharArrayRangesFilter filter = new CharArrayRangesFilter(12);
			filter.add(i, encoded.length, -10);
			
			StringBuilder b = new StringBuilder();
			
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
