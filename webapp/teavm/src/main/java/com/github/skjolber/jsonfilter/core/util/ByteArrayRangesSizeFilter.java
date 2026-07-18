package com.github.skjolber.jsonfilter.core.util;

import com.github.skjolber.jsonfilter.JsonFilterMetrics;
import com.github.skjolber.jsonfilter.ResizableByteArrayOutputStream;

/**
 * TeaVM-safe stub that shadows the real {@code ByteArrayRangesSizeFilter} from {@code impl/core}.
 *
 * <p>See {@link ByteArrayRangesFilter} for the rationale.  None of these methods are called
 * at runtime from the webapp — only the {@code process(String)} / {@code process(char[])}
 * path is used.
 */
public class ByteArrayRangesSizeFilter extends ByteArrayRangesFilter {

    private boolean[] squareBrackets;
    private int level;
    private int mark;
    private int maxSizeLimit;

    public ByteArrayRangesSizeFilter(int initialCapacity, int length,
            byte[] pruneMessage, byte[] anonymizeMessage, byte[] truncateMessage) {
        super(initialCapacity, length, pruneMessage, anonymizeMessage, truncateMessage);
    }

    public boolean[] grow(boolean[] squareBrackets) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public boolean[] getSquareBrackets() { return squareBrackets; }
    public void setSquareBrackets(boolean[] squareBrackets) { this.squareBrackets = squareBrackets; }

    public int getLevel()  { return level; }
    public void setLevel(int level)  { this.level = level; }

    public int getMark()   { return mark; }
    public void setMark(int mark)    { this.mark = mark; }

    public int getMaxSizeLimit()             { return maxSizeLimit; }
    public void setMaxSizeLimit(int limit)   { this.maxSizeLimit = limit; }

    public void closeStructure(ResizableByteArrayOutputStream output) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    @Override
    public void filter(byte[] chars, int offset, int length,
            ResizableByteArrayOutputStream buffer, JsonFilterMetrics metrics) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    @Override
    public void filter(byte[] chars, int offset, int length,
            ResizableByteArrayOutputStream buffer) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public int skipObjectOrArrayMaxSizeMaxStringLength(byte[] chars, int offset,
            int maxSizeLimit, int maxReadLimit, int maxStringLength) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }

    public int anonymizeSubtree(byte[] chars, int offset, int maxSizeLimit) {
        throw new UnsupportedOperationException("byte[] filter path not supported in TeaVM webapp");
    }
}
