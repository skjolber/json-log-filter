package com.github.skjolber.jsonfilter.core.util;

import java.nio.charset.StandardCharsets;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;
import com.github.skjolber.jsonfilter.base.AbstractRangesFilter;

/**
 * TeaVM-safe stub that shadows the real {@code ByteArrayRangesFilter} from {@code impl/core}.
 *
 * <p>The real implementation uses {@code java.lang.invoke.VarHandle} (for a word-at-a-time
 * quote scanner), which is absent from TeaVM's JRE emulation.  This stub provides the same
 * public API with no-op or throw bodies so that all classes that reference
 * {@code ByteArrayRangesFilter} compile cleanly.
 *
 * <p>None of these methods are ever called at runtime from the webapp: the entry point
 * calls {@code process(String)} which routes through the char-array path, never the
 * byte-array path.
 */
public class ByteArrayRangesFilter extends AbstractRangesFilter {

    protected static final byte[] DEFAULT_FILTER_PRUNE_MESSAGE_CHARS =
            FILTER_PRUNE_MESSAGE_JSON.getBytes(StandardCharsets.UTF_8);
    protected static final byte[] DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS =
            FILTER_ANONYMIZE_MESSAGE.getBytes(StandardCharsets.UTF_8);
    protected static final byte[] DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS =
            FILTER_TRUNCATE_MESSAGE.getBytes(StandardCharsets.UTF_8);

    // DigitTens / DigitOnes are referenced by writeInt callers
    protected static final byte[] DigitTens = new byte[100];
    protected static final byte[] DigitOnes = new byte[100];

    static {
        for (int i = 0; i < 100; i++) {
            DigitTens[i] = (byte) ('0' + i / 10);
            DigitOnes[i] = (byte) ('0' + i % 10);
        }
    }

    protected final byte[] pruneMessage;
    protected final byte[] anonymizeMessage;
    protected final byte[] truncateMessage;
    protected final byte[] digit = new byte[11];

    public ByteArrayRangesFilter(int initialCapacity, int length) {
        this(initialCapacity, length,
                DEFAULT_FILTER_PRUNE_MESSAGE_CHARS,
                DEFAULT_FILTER_ANONYMIZE_MESSAGE_CHARS,
                DEFAULT_FILTER_TRUNCATE_MESSAGE_CHARS);
    }

    public ByteArrayRangesFilter(int initialCapacity, int length,
            byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
        super(initialCapacity, length);
        this.pruneMessage = pruneMessage;
        this.anonymizeMessage = anonymizeMessage;
        this.truncateMessage = truncateMessage;
    }

    // -----------------------------------------------------------------------
    // Instance methods – byte[] filter path (never called from webapp)
    // -----------------------------------------------------------------------

    public void filter(byte[] chars, int offset, int length,
            ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public void filter(byte[] chars, int offset, int length,
            ResizableByteArrayOutputStream buffer) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public boolean addMaxLength(byte[] chars, int start, int end, int length) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public void addAnon(int start, int end) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public void addPrune(int start, int end) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public void addDelete(int start, int end) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public int getAnonymizeMessageLength() { return anonymizeMessage.length; }
    public int getPruneMessageLength()     { return pruneMessage.length; }

    public static boolean isHex(byte b) {
        int c = b & 0xFF;
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    // -----------------------------------------------------------------------
    // Static utility methods – used by ws and util byte-array filter classes.
    // All throw at runtime (never reached from the String/char[] path).
    // -----------------------------------------------------------------------

    public static int getStringAlignment(byte[] chars, int start) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int scanBeyondQuotedValue(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int scanQuotedValue(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int scanEscapedValue(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int scanBeyondUnquotedValue(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int skipObject(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int skipArray(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int skipObjectOrArray(byte[] chars, int offset) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static void writeInt(ResizableByteArrayOutputStream out, int v, byte[] digit) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int anonymizeObjectOrArray(byte[] chars, int offset, ByteArrayRangesFilter filter) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public static int skipObjectMaxStringLength(byte[] chars, int offset,
            int maxStringLength, ByteArrayRangesFilter filter) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }
}
