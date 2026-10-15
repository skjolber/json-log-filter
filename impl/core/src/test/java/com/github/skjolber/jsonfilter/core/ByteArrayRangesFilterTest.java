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
import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;
import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesSizeFilter;
import com.github.skjolber.jsonfilter.test.DefaultJsonFilterMetrics;

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

	private static final String TOP_LEVEL_STRING = "\"hello world, a long string\"";
	private static final String TOP_LEVEL_STRING_TRUNCATED = "\"hello... + 21\"";

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
		assertEquals(0, ByteArrayRangesFilter.addNonStructural(TOP_LEVEL_STRING.getBytes(StandardCharsets.UTF_8), 0, TOP_LEVEL_STRING.length(), Integer.MAX_VALUE, false, new ByteArrayRangesFilter(-1, TOP_LEVEL_STRING.length())).getFilterIndex());

		// one range per whitespace run, one for truncation
		String input = " " + TOP_LEVEL_STRING + "\n";
		assertEquals(9, ByteArrayRangesFilter.addNonStructural(input.getBytes(StandardCharsets.UTF_8), 0, input.length(), 5, true, new ByteArrayRangesFilter(-1, input.length())).getFilterIndex());

		// one range per whitespace run for a number
		input = " 123\n";
		assertEquals(6, ByteArrayRangesFilter.addNonStructural(input.getBytes(StandardCharsets.UTF_8), 0, input.length(), 1, true, new ByteArrayRangesFilter(-1, input.length())).getFilterIndex());
		assertEquals(0, ByteArrayRangesFilter.addNonStructural(input.getBytes(StandardCharsets.UTF_8), 0, input.length(), 1, false, new ByteArrayRangesFilter(-1, input.length())).getFilterIndex());
	}

	private static String filterNonStructural(String input, int maxStringLength, boolean removeWhitespace, JsonFilterMetrics metrics) {
		return filterNonStructural(input, 0, input.length(), maxStringLength, removeWhitespace, metrics);
	}

	private static String filterNonStructural(String input, int offset, int length, int maxStringLength, boolean removeWhitespace, JsonFilterMetrics metrics) {
		ResizableByteArrayOutputStream output = new ResizableByteArrayOutputStream(128);
		ByteArrayRangesFilter.filterNonStructural(input.getBytes(StandardCharsets.UTF_8), offset, length, output, metrics, maxStringLength, removeWhitespace, "\"[removed]\"".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8), "... + ".getBytes(StandardCharsets.UTF_8));
		return output.toString(StandardCharsets.UTF_8);
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
	public void testNoUnicodeAlignmentForBackspaceAndFormFeedEscapes() throws IOException {
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

			assertEquals(index, ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), index + 1));
			assertEquals(index + 2, ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), index + 2));
		}

		// an escaped backslash followed by a plain b or f: a cut before the letter stays
		for(String string : new String[] {"\"aaa\\\\bccc\"", "\"aaa\\\\fccc\""}) {
			assertEquals(6, ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), 6));
		}
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
	public void testEscapedSurrogatePairAlignment() throws IOException {
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
		byte[] bytes = "\"ab\\uD83D\\uDE00cd\"".getBytes(StandardCharsets.UTF_8);
		assertEquals(3, ByteArrayRangesFilter.getStringAlignment(bytes, 3));
		for(int start = 4; start < 15; start++) {
			assertEquals(3, ByteArrayRangesFilter.getStringAlignment(bytes, start), Integer.toString(start));
		}
		assertEquals(15, ByteArrayRangesFilter.getStringAlignment(bytes, 15));

		// a cut right after an escaped high surrogate (D800 to DBFF) moves to its start, whatever follows
		for(String escape : new String[] {"\\uD800", "\\uD83D", "\\uDBFF", "\\udbff"}) {
			String string = "\"ab" + escape + "\\u00e9cd\"";
			assertEquals(3, ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), 9), escape);
		}

		// a cut right after any other escape stays
		for(String escape : new String[] {"\\uD7FF", "\\uDC00", "\\uE000", "\\u0800"}) {
			String string = "\"ab" + escape + "\\u00e9cd\"";
			assertEquals(9, ByteArrayRangesFilter.getStringAlignment(string.getBytes(StandardCharsets.UTF_8), 9), escape);
		}

		// a cut after an escaped backslash followed by the plain text uD83D stays
		assertEquals(9, ByteArrayRangesFilter.getStringAlignment("\"a\\\\uD83Dxy\"".getBytes(StandardCharsets.UTF_8), 9));
	}

	@Test
	public void testIsHighSurrogateSecondHex() {
		for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
			boolean expected = i == '8' || i == '9' || i == 'A' || i == 'B' || i == 'a' || i == 'b';
			assertEquals(expected, ByteArrayRangesFilter.isHighSurrogateSecondHex((byte)i), Integer.toString(i));
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
	public void testOutputMetricsIncludeClosedStructure() {
		// cut within an array within an object: the closing brackets added by the size filter are output too
		byte[] bytes = "{\"a\":[1,2,3]}".getBytes(StandardCharsets.UTF_8);

		ByteArrayRangesSizeFilter filter = new ByteArrayRangesSizeFilter(12, bytes.length, "\"[removed]\"".getBytes(StandardCharsets.UTF_8), "\"*\"".getBytes(StandardCharsets.UTF_8), "...".getBytes(StandardCharsets.UTF_8));
		filter.addDelete(7, bytes.length);
		filter.setLevel(2);
		filter.getSquareBrackets()[1] = true;

		DefaultJsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
		ResizableByteArrayOutputStream b = new ResizableByteArrayOutputStream(128);
		filter.filter(bytes, 0, bytes.length, b, metrics);

		assertThat(b.toString()).isEqualTo("{\"a\":[1]}");
		assertEquals(b.size(), metrics.getOutputSize());
		assertEquals(bytes.length, metrics.getInputSize());
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
	public void testReplacementGrowth() {
		// set when prune or anonymize messages make the output larger than the input
		ByteArrayRangesSizeFilter filter = new ByteArrayRangesSizeFilter(-1, 100,
			"prune".getBytes(StandardCharsets.UTF_8),
			"anon".getBytes(StandardCharsets.UTF_8),
			"trunc".getBytes(StandardCharsets.UTF_8));
		assertFalse(filter.isReplacementGrowth());

		filter.setReplacementGrowth(true);
		assertTrue(filter.isReplacementGrowth());

		filter.setReplacementGrowth(false);
		assertFalse(filter.isReplacementGrowth());
	}

	@Test
	public void testScanQuotedValueScalarWithDoubleBackslashBeforeClose() {
		// A short string ending with a double-backslash causes the scalar path to detect a backslash
		// before the closing quote and hand off to the escaped-value scanner.
		byte[] shortBuf = new byte[]{ '"', 'a', 'b', '\\', '\\', '"' };
		int result = ByteArrayRangesFilter.scanQuotedValue(shortBuf, 0);
		assertEquals(5, result); // closing " at index 5

		// A string containing an escaped quote followed by enough content to exercise the
		// bulk scan path within the escaped-value scanner.
		String escapedContent = "aaaaaaa\\\"bbbbbbbbbb";
		byte[] medBuf = ('"' + escapedContent + '"').getBytes(StandardCharsets.UTF_8);
		int result2 = ByteArrayRangesFilter.scanQuotedValue(medBuf, 0);
		assertEquals(medBuf.length - 1, result2);
	}

	@Test
	public void testSkipObjectMaxStringLengthWithFalseValue() {
		// An object containing a JSON false value is skipped correctly when the string length limit is active.
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(100, 100);
		byte[] json = "{\"flag\":false,\"other\":true}".getBytes(StandardCharsets.UTF_8);
		int result = ByteArrayRangesFilter.skipObjectMaxStringLength(json, 1, 10, filter);
		assertTrue(result > 0);
	}

	@Test
	public void testWriteIntNegative() {
		// Writing a negative integer to the output buffer must produce the correct sign and digits.
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(10, 100);
		ResizableByteArrayOutputStream buffer = new ResizableByteArrayOutputStream(20);
		filter.writeInt(buffer, -12345);
		String result = buffer.toString(java.nio.charset.StandardCharsets.ISO_8859_1);
		assertTrue(result.contains("-12345"));
	}

	@Test
	public void testAddMaxLengthNegativeLength() {
		// Specifying a negative maximum string length must throw an IllegalArgumentException.
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(10, 100);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> filter.addMaxLength(new byte[10], 0, 5, -1));
	}

	@Test
	public void testFilterWithMetricsNullMetrics() throws Exception {
		// filter(byte[], int, int, ResizableByteArrayOutputStream, JsonFilterMetrics) must handle
		// null metrics without NPE, exercising the 'metrics == null' branches (lines 135-181).
		String json = "{\"key\":\"value\",\"num\":42}";
		byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		ByteArrayRangesFilter filter = new ByteArrayRangesFilter(12, bytes.length);
		filter.addAnon(7, 14);
		ResizableByteArrayOutputStream buffer = new ResizableByteArrayOutputStream(64);
		filter.filter(bytes, 0, bytes.length, buffer, null);
		// Verify something was written (the anon message replaced part of the JSON)
		assertTrue(buffer.size() > 0);
		String result = new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(result.contains("\"*\""));
	}

}
