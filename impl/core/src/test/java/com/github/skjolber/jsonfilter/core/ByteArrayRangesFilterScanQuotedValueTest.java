package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter;

/**
 * Full-coverage tests for {@link ByteArrayRangesFilter#scanQuotedValue}.
 *
 * <p>Strategy: every case is validated by comparing the optimised implementation
 * against the original scalar reference extracted verbatim from the pre-optimisation
 * baseline (git master).  Any divergence is a correctness regression.
 *
 * <h2>Reference implementation</h2>
 * <p>The {@link #referenceScalarScanQuotedValue} and
 * {@link #referenceScalarScanEscapedValue} methods are verbatim copies of the
 * scalar implementation that existed before the word-at-a-time optimisation
 * (PR #244 / PR #245).  They serve as the ground-truth oracle.
 *
 * <h2>Optimisation under test</h2>
 * <p>The production implementation uses the <em>has-zero-byte</em> trick from:
 * <blockquote>
 *   Henry S. Warren Jr., <em>Hacker's Delight</em>, 2nd ed. (2012),
 *   Addison-Wesley Professional, ISBN 978-0-321-84268-8, §6-1
 *   "Find First 0-Byte", p. 117.
 * </blockquote>
 * combined with a dual VarHandle {@code getLong} per iteration (16 bytes/iter).
 * See the {@link ByteArrayRangesFilter} class Javadoc for the full formula.
 *
 * <h2>Test-case coverage</h2>
 * <ul>
 *   <li>Scalar tail (string content &lt; 8 bytes)</li>
 *   <li>8-byte VarHandle loop (8–15 bytes of content)</li>
 *   <li>16-byte dual-load loop (≥ 16 bytes of content)</li>
 *   <li>Quote at every byte position within an 8-byte word (0–7)</li>
 *   <li>Quote at every byte position within a 16-byte window (0–15)</li>
 *   <li>Escaped quote {@code \"} at each of the above positions</li>
 *   <li>Even/odd backslash counts ({@code \\"} terminates, {@code \\\"} does not)</li>
 *   <li>Multiple escaped quotes before the real closing quote</li>
 *   <li>Large strings (32–64 bytes of content)</li>
 * </ul>
 */
class ByteArrayRangesFilterScanQuotedValueTest {

    // -------------------------------------------------------------------------
    // Baseline reference implementation for verifying correctness of optimised variants.
    // -------------------------------------------------------------------------

    /** Verbatim copy of scanQuotedValue before the word-at-a-time optimisation. */
    private static int referenceScalarScanQuotedValue(final byte[] chars, int offset) {
        while (chars[++offset] != '"') ;
        if (chars[offset - 1] != '\\') {
            return offset;
        }
        return referenceScalarScanEscapedValue(chars, offset);
    }

    /** Verbatim copy of scanEscapedValue before the word-at-a-time optimisation. */
    private static int referenceScalarScanEscapedValue(final byte[] chars, int offset) {
        while (true) {
            // is there an even number of slashes behind the last '"'?
            int slashOffset = offset - 2;
            while (chars[slashOffset] == '\\') {
                slashOffset--;
            }
            if ((offset - slashOffset) % 2 == 1) {
                return offset;
            }
            while (chars[++offset] != '"') ;
            if (chars[offset - 1] != '\\') {
                return offset;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a byte[] containing {@code "} + rawContent + {@code "} followed by
     * 32 bytes of padding.  The padding ensures that VarHandle reads 8 or 16 bytes
     * ahead never go out of bounds, mirroring the real-world scenario where the
     * filter always operates on a larger JSON document buffer.
     *
     * @param rawContent bytes placed between the opening and closing quote
     * @return array where index 0 is the opening {@code "}
     */
    private static byte[] quoted(byte[] rawContent) {
        byte[] buf = new byte[1 + rawContent.length + 1 + 32];
        buf[0] = '"';
        System.arraycopy(rawContent, 0, buf, 1, rawContent.length);
        buf[1 + rawContent.length] = '"';
        // padding bytes are 0x00 — not '"' so they don't confuse the scanner
        return buf;
    }

    private static byte[] quoted(String content) {
        return quoted(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds content of exactly {@code length} non-quote ASCII bytes (a, b, c, …). */
    private static String plain(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }

    /**
     * Inserts an escaped quote {@code \"} at byte position {@code pos} within
     * {@code length} bytes of content, with plain ASCII on either side.
     * The resulting content is {@code pos} plain bytes + {@code \"} + remaining plain bytes.
     */
    private static String withEscapedQuoteAt(int contentLength, int escapedQuotePos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < escapedQuotePos; i++) sb.append('a');
        sb.append("\\\"");
        int remaining = contentLength - escapedQuotePos;
        for (int i = 0; i < remaining; i++) sb.append('b');
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Test-case providers
    // -------------------------------------------------------------------------

    static Stream<TestCase> plainStrings() {
        List<TestCase> cases = new ArrayList<>();
        // Length 0 → 35: covers scalar tail (0-7), 8-byte loop (8-15), 16-byte loop (16+)
        for (int len = 0; len <= 35; len++) {
            String label = "plain[" + len + "]";
            cases.add(new TestCase(label, quoted(plain(len))));
        }
        // Extra large strings to exercise multiple 16-byte iterations
        for (int len : new int[]{48, 63, 64, 65, 100, 128, 256}) {
            cases.add(new TestCase("plain[" + len + "]", quoted(plain(len))));
        }
        return cases.stream();
    }

    static Stream<TestCase> quotePositionWithinWord() {
        // For each word-offset (0–7), place the closing quote there by choosing
        // content lengths that map to that byte position within the current word.
        List<TestCase> cases = new ArrayList<>();
        // Scalar tail: lengths 0-7 already covered by plainStrings, but explicit here
        for (int wordPos = 0; wordPos < 8; wordPos++) {
            // Position within an 8-byte window inside the 8-byte loop
            int len = 8 + wordPos;  // content starts at i=1; first full word at i=1..8
            cases.add(new TestCase("8byteLoop_wordPos" + wordPos, quoted(plain(len))));
            // Position within the 16-byte dual-load loop
            int len16 = 16 + wordPos;
            cases.add(new TestCase("16byteLoop_word1_pos" + wordPos, quoted(plain(len16))));
            // Position within the second word of the 16-byte load
            int len16w2 = 24 + wordPos;
            cases.add(new TestCase("16byteLoop_word2_pos" + wordPos, quoted(plain(len16w2))));
        }
        return cases.stream();
    }

    static Stream<TestCase> escapedQuotes() {
        List<TestCase> cases = new ArrayList<>();

        // Single escaped quote at various positions throughout all three loop regions
        int[] escapedQuotePositions = intArray(40);
        for (int eqPos : escapedQuotePositions) {
            // Content: eqPos plain bytes + \" + 8 more plain bytes (real close at the end)
            String content = withEscapedQuoteAt(eqPos + 8, eqPos);
            cases.add(new TestCase("escapedQuoteAt[" + eqPos + "]", quoted(content)));
        }

        // Double backslash immediately before the closing quote: \\" → terminates
        // (the \\ is an escaped backslash; the " is unescaped and closes the string)
        for (int prefixLen : intArray(40)) {
            String content = plain(prefixLen) + "\\\\";
            cases.add(new TestCase("doubleBackslashClose_prefix" + prefixLen, quoted(content)));
        }

        // Escaped backslash then escaped quote: \\\": should NOT terminate on the \" 
        // The \\\ sequence is: escaped backslash (\) + start of next escape, 
        // so \\\" = (\\)(\") = backslash literal + escaped quote (not a string end)
        for (int prefixLen : intArray(40)) {
            String content = plain(prefixLen) + "\\\\\\\"" + plain(8);
            cases.add(new TestCase("escapedBackslashThenEscapedQuote_prefix" + prefixLen, quoted(content)));
        }

        // Multiple escaped quotes before the real closing quote
        cases.add(new TestCase("multipleEscapedQuotes_short",  quoted("abc\\\"def\\\"ghi")));
        cases.add(new TestCase("multipleEscapedQuotes_across8", quoted("abcdefg\\\"hijklmno\\\"pqrstuv")));
        cases.add(new TestCase("multipleEscapedQuotes_across16", quoted("abcdefghijklmno\\\"pqrstuvwxyzabc\\\"ABCDE")));

        return cases.stream();
    }

    private static int[] intArray(int length) {
    	int[] result = new int[length];
    	for(int i = 0; i < length; i++) {
    		result[i] = i;
    	}
		return result;
	}

	static Stream<TestCase> emptyAndSingleChar() {
        return Stream.of(
            new TestCase("empty",          quoted("")),
            new TestCase("singleA",        quoted("a")),
            new TestCase("singleBackslash",quoted("a\\\\"))  // content: a\\ (escaped backslash, then close)
        );
    }

    static Stream<TestCase> allCases() {
        return Stream.of(
            plainStrings(),
            quotePositionWithinWord(),
            escapedQuotes(),
            emptyAndSingleChar()
        ).flatMap(s -> s);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCases")
    void scanQuotedValue_matchesScalarReference(TestCase tc) {
        int expected = referenceScalarScanQuotedValue(tc.bytes, 0);
        int actual   = ByteArrayRangesFilter.scanQuotedValue(tc.bytes, 0);
        assertEquals(expected, actual,
            () -> "scanQuotedValue mismatch for case '" + tc.name + "': bytes=" + describe(tc.bytes));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCases")
    void scanBeyondQuotedValue_matchesScalarReference(TestCase tc) {
        int expected = referenceScalarScanQuotedValue(tc.bytes, 0) + 1;
        int actual   = ByteArrayRangesFilter.scanBeyondQuotedValue(tc.bytes, 0);
        assertEquals(expected, actual,
            () -> "scanBeyondQuotedValue mismatch for case '" + tc.name + "'");
    }

    // -------------------------------------------------------------------------
    // Direct scanEscapedValue tests
    //
    // scanEscapedValue is called when a '"' preceded by '\' is found; it decides
    // whether that backslash actually escapes the quote (odd backslash count) or
    // is itself escaped (even backslash count), and advances to the real closing '"'.
    // -------------------------------------------------------------------------

    static Stream<EscapeCase> escapeCases() {
        List<EscapeCase> cases = new ArrayList<>();

        // Helper: build array that starts at an already-found '"', with context before it.
        // The array layout is: padding + prefix + '"' (the escaped candidate) + rest + '"' (real close) + padding
        // We call scanEscapedValue(buf, quoteIdx) directly.

        // Case 1: single backslash → quote is escaped, advance to next real '"'
        // buf: \  "  a  b  c  "  (padding follows)
        cases.add(escapeCaseOf("singleBackslash_shortSuffix", "\\\"abc\"", 1));

        // Case 2: double backslash before '"' → backslash is escaped, '"' closes the string
        // buf: \\ " (real close)
        cases.add(escapeCaseOf("doubleBackslash_immediate", "\\\\\"", 2));

        // Case 3: triple backslash before '"' → (\\)(\") = escaped backslash + escaped quote, advance
        cases.add(escapeCaseOf("tripleBackslash_longSuffix", "\\\\\\\"abcdefghij\"", 3));

        // Case 4: escaped quote then real close within 8-byte loop range
        cases.add(escapeCaseOf("escapedQuote_in8ByteRange", "\\\"abcdefg\"", 1));

        // Case 5: escaped quote then real close within 16-byte dual-load range
        cases.add(escapeCaseOf("escapedQuote_in16ByteRange", "\\\"abcdefghijklmno\"", 1));

        // Case 6: multiple escaped quotes before the real closing quote
        cases.add(escapeCaseOf("multipleEscapedQuotes", "\\\"abc\\\"def\\\"ghi\"", 1));

        // Case 7: escaped quote right at an 8-byte word boundary
        cases.add(escapeCaseOf("escapedQuote_at8ByteBoundary", "\\\"aaaaaaa\\\"bbbbbbb\"", 1));

        // Case 8: escaped quote right at a 16-byte boundary (second word)
        cases.add(escapeCaseOf("escapedQuote_at16ByteBoundary", "\\\"aaaaaaaaaaaaaaa\\\"bbb\"", 1));

        // Case 9: real close immediately after the candidate escaped quote (odd slash count)
        cases.add(escapeCaseOf("realClose_afterOddSlashes", "\\\\\\\"\"", 3));

        return cases.stream();
    }

    /**
     * Build an EscapeCase where {@code raw} is the bytes after the opening {@code "},
     * and {@code quoteIdx} is the index within {@code raw} of the first candidate '"'
     * (the one we pass to scanEscapedValue).  The full buffer is:
     * {@code  " } + raw + {@code  "} (padding).
     *
     * <p>The opening {@code "} is at index 0; quoteIdx+1 is the candidate quote position.
     */
    private static EscapeCase escapeCaseOf(String name, String raw, int quoteIdx) {
        byte[] rawBytes = raw.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Buffer: opening " + rawBytes + closing " + 32 padding bytes
        byte[] buf = new byte[1 + rawBytes.length + 32];
        buf[0] = '"';
        System.arraycopy(rawBytes, 0, buf, 1, rawBytes.length);
        // Ensure there's a '"' at the end of the real content (raw already contains it)
        return new EscapeCase(name, buf, 1 + quoteIdx);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("escapeCases")
    void scanEscapedValue_matchesScalarReference(EscapeCase ec) {
        int expected = referenceScalarScanEscapedValue(ec.bytes, ec.quoteIdx);
        int actual   = ByteArrayRangesFilter.scanEscapedValue(ec.bytes, ec.quoteIdx);
        assertEquals(expected, actual,
            () -> "scanEscapedValue mismatch for case '" + ec.name + "': bytes=" + describe(ec.bytes));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String describe(byte[] bytes) {
        // Show only the meaningful prefix (up to the real closing quote + a few bytes)
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(bytes.length, 48); i++) {
            byte b = bytes[i];
            if (b >= 0x20 && b < 0x7f) sb.append((char) b);
            else sb.append(String.format("\\x%02x", b));
        }
        if (bytes.length > 48) sb.append("...");
        sb.append("]");
        return sb.toString();
    }

    record TestCase(String name, byte[] bytes) {
        @Override
        public String toString() { return name; }
    }

    record EscapeCase(String name, byte[] bytes, int quoteIdx) {
        @Override
        public String toString() { return name; }
    }
}
