package com.github.skjolber.jsonfilter.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;

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
					
			assertThat(ByteArrayRangesFilter.scanBeyondUnquotedValue(terminatedValue.getBytes(), 0)).isEqualTo(terminatedValue.length() - 1);
		}
	}

	@Test
	public void testSimpleEscapeAlignment() throws IOException {
		byte[] encoded = "abcdef\\nghijkl".getBytes(StandardCharsets.UTF_8);
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			
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
			
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			
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

			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			
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
				
				ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
				filter.filter(encoded, 0, encoded.length, b);
				String string = b.toString();
				assertEquals(string.length(), prefix.length() + "... + XX".length());
			}

			// make sure last included character is byte 2, 3 or 4
			// to force a check which concludes that the whole unicode character
			// (i.e. all the bytes) are included
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, prefix.length() + unicodeBytes.length, encoded.length, encoded.length);
			
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
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
			
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			
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
		
		ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
		
		filter.filter(encoded, 0, encoded.length, b);
		return b.toString();
	}
	
	@Test
	public void testNoUnicodeAlignmentForSingleEscapeCharacters() throws IOException {
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
	public void testNoUnicodeAlignmentForSingleInlinedUnicodeCharacters() throws IOException {
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
	public void testSurrugateAlignment() throws IOException {
		String escaped = "\uF234";
		byte[] encoded = ("abcdefghi" + escaped + "ghijkl").getBytes(StandardCharsets.UTF_8);
		
		for(int i = 2; i < encoded.length; i++) {

			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, encoded.length);
			filter.addMaxLength(encoded, i, encoded.length, encoded.length - i);
			
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			
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
		ByteArrayRangesFilter.anonymizeObjectOrArray(input.getBytes(), 0, filter);
		
		ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
		filter.filter(input.getBytes(), 0, input.length(), b);
		
		assertThat(b.toString()).isEqualTo(output);
	}

	@Test
	public void testPruneSubtreeScalar() throws IOException {
		String[] inputs = new String[]{"\"abcde\",", "\"abcde\"}"};
		String[] outputs = new String[]{"\"[removed]\",", "\"[removed]\"}"};
		
		for(int i = 0; i < inputs.length; i++) {
			String input = inputs[i];
			
			ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, input.length());
			filter.addPrune(0, input.length() -1);
			ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
			filter.filter(input.getBytes(), 0, input.length(), b);
			
			assertThat(b.toString()).isEqualTo(outputs[i]);
		}
	}

	@Test
	public void testSkipArray() {
		byte[] json = "[1,2,3]x".getBytes(StandardCharsets.UTF_8);
		int end = ByteArrayRangesFilter.skipArray(json, 0);
		assertEquals(7, end);

		byte[] nested = "[[1,2],[3,4]]x".getBytes(StandardCharsets.UTF_8);
		end = ByteArrayRangesFilter.skipArray(nested, 0);
		assertEquals(13, end);
	}

	@Test
	public void testSetSquareBrackets() {
		ByteArrayRangesSizeFilter filter = new ByteArrayRangesSizeFilter(-1, 100,
			"prune".getBytes(StandardCharsets.UTF_8),
			"anon".getBytes(StandardCharsets.UTF_8),
			"trunc".getBytes(StandardCharsets.UTF_8));
		boolean[] brackets = new boolean[]{true, false, true};
		filter.setSquareBrackets(brackets);
		assertSame(brackets, filter.getSquareBrackets());
	}

	@Test
	public void testScanQuotedValueScalarWithDoubleBackslashBeforeClose() {
		// Line 487 in ByteArrayRangesFilter.scanQuotedValue:
		//   `return scanEscapedValue(chars, i)` in the scalar tail when chars[i-1] == '\\'
		// Need a SHORT byte array (no padding) so 16-byte and 8-byte loops are skipped.
		// String "ab\\" = open + a + b + \ + \ + close = 6 bytes total.
		// safeEnd8=6-8=-2, safeEnd16=6-16=-10 → both loops skipped → scalar loop used.
		// Scalar: i goes 1,2,3,4,5(=close). chars[4]='\' → scanEscapedValue called (line 487).
		byte[] shortBuf = new byte[]{ '"', 'a', 'b', '\\', '\\', '"' };
		int result = ByteArrayRangesFilter.scanQuotedValue(shortBuf, 0);
		assertEquals(5, result); // closing " at index 5

		// Lines 529-530 in ByteArrayRangesFilter.scanEscapedValue: `i += 8` in 8-byte scan
		// Need a string where after escaped quote, the remaining content is 8+ bytes,
		// so the 8-byte scan runs at least one iteration with i+=8 (no quote found).
		// String: "aaaaaaa\"bbbbbbbbbb" (7 a's + \" + 10 b's + closing ")
		// = 7 + 2 + 10 + 2 = 21 bytes. After \" at position 8-9: remaining "bbbbbbbbbb" + "
		// is 11 bytes. 8-byte scan runs once with i+=8 before finding closing ".
		String escapedContent = "aaaaaaa\\\"bbbbbbbbbb";
		byte[] medBuf = ('"' + escapedContent + '"').getBytes(StandardCharsets.UTF_8);
		int result2 = ByteArrayRangesFilter.scanQuotedValue(medBuf, 0);
		assertEquals(medBuf.length - 1, result2);
	}

	@Test
	public void testSkipObjectMaxStringLengthWithFalseValue() {
		// Line 764-765 in ByteArrayRangesFilter.skipObjectMaxStringLength: case 'f' (false)
		// Need to call skipObjectMaxStringLength with an object containing "false" value
		// skipObjectMaxStringLength is package-private, so use it via a filter that calls it
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(100, 100);
		// Directly test skipObjectMaxStringLength which contains case 'f'
		byte[] json = "{\"flag\":false,\"other\":true}".getBytes(StandardCharsets.UTF_8);
		int result = ByteArrayRangesFilter.skipObjectMaxStringLength(json, 1, 10, filter);
		assertTrue(result > 0);
	}

	@Test
	public void testWriteIntNegative() {
		// getChars() is only invoked from writeInt(). Passing a negative value exercises
		// the negative-number branch in getChars (lines 73-74, 97-98 in ByteArrayRangesFilter).
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(10, 100);
		ResizableByteArrayOutputStream buffer = new ResizableByteArrayOutputStream(20);
		filter.writeInt(buffer, -12345);
		String result = buffer.toString(java.nio.charset.StandardCharsets.ISO_8859_1);
		assertTrue(result.contains("-12345"));
	}

	@Test
	public void testAddMaxLengthNegativeLength() {
		// addMaxLength() throws IllegalArgumentException for negative length (lines 203-204).
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(10, 100);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> filter.addMaxLength(new byte[10], 0, 5, -1));
	}

}
