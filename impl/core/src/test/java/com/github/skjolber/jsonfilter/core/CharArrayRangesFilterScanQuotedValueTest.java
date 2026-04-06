package com.github.skjolber.jsonfilter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.skjolber.jsonfilter.core.util.CharArrayRangesFilter;

/**
 * Full-coverage tests for {@link CharArrayRangesFilter#scanQuotedValue} and
 * {@link CharArrayRangesFilter#scanEscapedValue}.
 *
 * <p>Strategy: every case is validated by comparing the production implementation
 * against an inline scalar reference.  The reference methods capture the correct
 * semantics; the production code must agree on every input.  Future optimisations
 * (e.g. word-at-a-time) must continue to match this oracle.
 *
 * <h2>Reference implementation</h2>
 * <p>The {@link #referenceScalarScanQuotedValue} and
 * {@link #referenceScalarScanEscapedValue} methods are verbatim copies of the
 * scalar implementation and serve as the ground-truth oracle.
 *
 * <h2>Test-case coverage</h2>
 * <ul>
 *   <li>Empty string and single-char strings</li>
 *   <li>Short strings (content &lt; 4 chars)</li>
 *   <li>Medium strings (4–15 chars of content)</li>
 *   <li>Closing quote at each char position within groups of 4 (0–3)</li>
 *   <li>Escaped quote {@code \"} at each of the above positions</li>
 *   <li>Even/odd backslash counts ({@code \\"} terminates, {@code \\\"} does not)</li>
 *   <li>Multiple escaped quotes before the real closing quote</li>
 *   <li>Large strings (32–64 chars of content)</li>
 *   <li>Direct {@code scanEscapedValue} tests covering the backslash-count logic</li>
 * </ul>
 */
class CharArrayRangesFilterScanQuotedValueTest {

    // -------------------------------------------------------------------------
    // Reference implementation (scalar oracle)
    // -------------------------------------------------------------------------

    /** Scalar oracle for scanQuotedValue — ground truth for all test assertions. */
    private static int referenceScalarScanQuotedValue(final char[] chars, int offset) {
        while (chars[++offset] != '"') ;
        if (chars[offset - 1] != '\\') {
            return offset;
        }
        return referenceScalarScanEscapedValue(chars, offset);
    }

    /** Scalar oracle for scanEscapedValue — ground truth for all test assertions. */
    private static int referenceScalarScanEscapedValue(final char[] chars, int offset) {
        while (true) {
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
     * Builds a {@code char[]} containing {@code "} + rawContent + {@code "} followed
     * by padding chars.  The padding ensures scanners that read ahead (e.g. a future
     * word-at-a-time optimisation) never go out of bounds.
     *
     * @param rawContent chars placed between the opening and closing quote
     * @return array where index 0 is the opening {@code "}
     */
    private static char[] quoted(char[] rawContent) {
        char[] buf = new char[1 + rawContent.length + 1 + 16];
        buf[0] = '"';
        System.arraycopy(rawContent, 0, buf, 1, rawContent.length);
        buf[1 + rawContent.length] = '"';
        // padding chars are '\0' — not '"' so they don't confuse the scanner
        return buf;
    }

    private static char[] quoted(String content) {
        return quoted(content.toCharArray());
    }

    /** Builds content of exactly {@code length} non-quote ASCII chars (a, b, c, …). */
    private static String plain(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }

    /**
     * Inserts an escaped quote {@code \"} at char position {@code pos} within
     * {@code length} chars of content, with plain ASCII on either side.
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
        // Length 0–19: covers short strings (0-3), medium strings (4-7), longer strings (8+)
        for (int len = 0; len <= 19; len++) {
            cases.add(new TestCase("plain[" + len + "]", quoted(plain(len))));
        }
        // Extra large strings to exercise many 4-char iterations
        for (int len : new int[]{32, 47, 48, 63, 64, 65, 100}) {
            cases.add(new TestCase("plain[" + len + "]", quoted(plain(len))));
        }
        return cases.stream();
    }

    static Stream<TestCase> quoteAtEachCharPosition() {
        // Closing quote at each char position within successive groups of 4 (0–3).
        // Verifies correct termination regardless of where the quote lands relative
        // to any aligned read window a future optimisation might use.
        List<TestCase> cases = new ArrayList<>();
        for (int groupPos = 0; groupPos < 4; groupPos++) {
            // First group of 4 (content length 4–7)
            int len4 = 4 + groupPos;
            cases.add(new TestCase("group1_pos" + groupPos, quoted(plain(len4))));
            // Second group of 4 (content length 8–11)
            int len8 = 8 + groupPos;
            cases.add(new TestCase("group2_pos" + groupPos, quoted(plain(len8))));
            // Third group of 4 (content length 12–15)
            int len12 = 12 + groupPos;
            cases.add(new TestCase("group3_pos" + groupPos, quoted(plain(len12))));
        }
        return cases.stream();
    }

    static Stream<TestCase> escapedQuotes() {
        List<TestCase> cases = new ArrayList<>();

        // Single escaped quote at positions spanning all loop regions
        int[] escapedQuotePositions = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 15, 16, 20};
        for (int eqPos : escapedQuotePositions) {
            String content = withEscapedQuoteAt(eqPos + 4, eqPos);
            cases.add(new TestCase("escapedQuoteAt[" + eqPos + "]", quoted(content)));
        }

        // Double backslash before closing quote: \\" → terminates
        for (int prefixLen : new int[]{0, 2, 3, 4, 7, 8, 11, 12}) {
            String content = plain(prefixLen) + "\\\\";
            cases.add(new TestCase("doubleBackslashClose_prefix" + prefixLen, quoted(content)));
        }

        // Escaped backslash then escaped quote: \\\" → backslash literal + escaped quote (no close)
        for (int prefixLen : new int[]{0, 3, 4, 7, 8}) {
            String content = plain(prefixLen) + "\\\\\\\"" + plain(4);
            cases.add(new TestCase("escapedBackslashThenEscapedQuote_prefix" + prefixLen, quoted(content)));
        }

        // Multiple escaped quotes before the real closing quote
        cases.add(new TestCase("multipleEscapedQuotes_short",   quoted("ab\\\"cd\\\"ef")));
        cases.add(new TestCase("multipleEscapedQuotes_across4",  quoted("abc\\\"defg\\\"hi")));
        cases.add(new TestCase("multipleEscapedQuotes_across8",  quoted("abcdefg\\\"hijklmn\\\"opqrstu")));
        cases.add(new TestCase("multipleEscapedQuotes_across16", quoted("abcdefghijklmno\\\"pqrstuvwxyzab\\\"ABCDE")));

        return cases.stream();
    }

    static Stream<TestCase> emptyAndSingleChar() {
        return Stream.of(
            new TestCase("empty",           quoted("")),
            new TestCase("singleA",         quoted("a")),
            new TestCase("singleBackslash", quoted("a\\\\"))  // content: a\\ (escaped backslash, then close)
        );
    }

    static Stream<TestCase> allCases() {
        return Stream.of(
            plainStrings(),
            quoteAtEachCharPosition(),
            escapedQuotes(),
            emptyAndSingleChar()
        ).flatMap(s -> s);
    }

    // -------------------------------------------------------------------------
    // scanQuotedValue / scanBeyondQuotedValue tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCases")
    void scanQuotedValue_matchesScalarReference(TestCase tc) {
        int expected = referenceScalarScanQuotedValue(tc.chars, 0);
        int actual   = CharArrayRangesFilter.scanQuotedValue(tc.chars, 0);
        assertEquals(expected, actual,
            () -> "scanQuotedValue mismatch for case '" + tc.name + "': chars=" + describe(tc.chars));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCases")
    void scanBeyondQuotedValue_matchesScalarReference(TestCase tc) {
        int expected = referenceScalarScanQuotedValue(tc.chars, 0) + 1;
        int actual   = CharArrayRangesFilter.scanBeyondQuotedValue(tc.chars, 0);
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

        // Case 1: single backslash → quote is escaped, advance to next real '"'
        cases.add(escapeCaseOf("singleBackslash_shortSuffix", "\\\"abc\"", 1));

        // Case 2: double backslash before '"' → backslash is escaped, '"' closes the string
        cases.add(escapeCaseOf("doubleBackslash_immediate", "\\\\\"", 2));

        // Case 3: triple backslash → (\\)(\") = escaped backslash + escaped quote, advance
        cases.add(escapeCaseOf("tripleBackslash_longSuffix", "\\\\\\\"abcdefghij\"", 3));

        // Case 4: escaped quote, real close within 4-char word range
        cases.add(escapeCaseOf("escapedQuote_in4CharRange", "\\\"abc\"", 1));

        // Case 5: escaped quote, real close within 8-char range (second word)
        cases.add(escapeCaseOf("escapedQuote_in8CharRange", "\\\"abcdefg\"", 1));

        // Case 6: escaped quote, real close within 16-char range
        cases.add(escapeCaseOf("escapedQuote_in16CharRange", "\\\"abcdefghijklmno\"", 1));

        // Case 7: multiple escaped quotes before the real closing quote
        cases.add(escapeCaseOf("multipleEscapedQuotes", "\\\"abc\\\"def\\\"ghi\"", 1));

        // Case 8: escaped quote right at a 4-char word boundary
        cases.add(escapeCaseOf("escapedQuote_at4CharBoundary", "\\\"aaa\\\"bbbbb\"", 1));

        // Case 9: real close immediately after odd-slash sequence
        cases.add(escapeCaseOf("realClose_afterOddSlashes", "\\\\\\\"\"", 3));

        return cases.stream();
    }

    /**
     * Build an EscapeCase where {@code raw} is the chars after the opening {@code "},
     * and {@code quoteIdx} is the index within {@code raw} of the first candidate '"'
     * (the one we pass to {@code scanEscapedValue}).
     *
     * <p>Buffer layout: {@code "} + raw + 16 padding chars.
     * The opening {@code "} is at index 0; {@code quoteIdx + 1} is the candidate position.
     */
    private static EscapeCase escapeCaseOf(String name, String raw, int quoteIdx) {
        char[] rawChars = raw.toCharArray();
        char[] buf = new char[1 + rawChars.length + 16];
        buf[0] = '"';
        System.arraycopy(rawChars, 0, buf, 1, rawChars.length);
        return new EscapeCase(name, buf, 1 + quoteIdx);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("escapeCases")
    void scanEscapedValue_matchesScalarReference(EscapeCase ec) {
        int expected = referenceScalarScanEscapedValue(ec.chars, ec.quoteIdx);
        int actual   = CharArrayRangesFilter.scanEscapedValue(ec.chars, ec.quoteIdx);
        assertEquals(expected, actual,
            () -> "scanEscapedValue mismatch for case '" + ec.name + "': chars=" + describe(ec.chars));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String describe(char[] chars) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(chars.length, 48); i++) {
            char c = chars[i];
            if (c >= 0x20 && c < 0x7f) sb.append(c);
            else if (c == 0) sb.append("\\0");
            else sb.append(String.format("\\u%04x", (int) c));
        }
        if (chars.length > 48) sb.append("...");
        sb.append("]");
        return sb.toString();
    }

    record TestCase(String name, char[] chars) {
        @Override
        public String toString() { return name; }
    }

    record EscapeCase(String name, char[] chars, int quoteIdx) {
        @Override
        public String toString() { return name; }
    }
}
