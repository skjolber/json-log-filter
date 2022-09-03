package com.github.skjolber.jsonfilter.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

public class FlexibleOutputStream extends ByteArrayOutputStream {

	private static final int MIN_INCREMENT = 16 * 1024;

	private final int maxSize;

    /**
     * The buffer where data is stored.
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer.
     */
    protected int count;

    /**
     * Creates a new {@code ByteArrayOutputStream}, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param  size   the initial size.
     * @throws IllegalArgumentException if size is negative.
     */
    public FlexibleOutputStream(int initialSize, int maxSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                                               + initialSize);
        }
        this.buf = new byte[initialSize];
        this.maxSize = maxSize;
    }

    public void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity > buf.length) {
        	// we need some more space
        	
        	int minimumIncrement = minCapacity - buf.length;
        	if(minimumIncrement < MIN_INCREMENT) {
        		minimumIncrement = MIN_INCREMENT;
        	}
        	
        	// add half-way to max size, or a minimum amount
        	if(minCapacity < maxSize) {
        		
        		int halfwayToMaxSize = (minCapacity + maxSize) / 2;
        		
                buf = copyOf(buf, Math.max(halfwayToMaxSize, buf.length + minimumIncrement));
        	} else {
        		// minimum amount
                buf = copyOf(buf, buf.length + minimumIncrement);
        	}
        }
    }

    /**
     * Writes the specified byte to this {@code ByteArrayOutputStream}.
     *
     * @param   b   the byte to be written.
     */
    public synchronized void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this {@code ByteArrayOutputStream}.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @throws  IndexOutOfBoundsException if {@code off} is negative,
     * {@code len} is negative, or {@code len} is greater than
     * {@code b.length - off}
     */
    public synchronized void write(byte b[], int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes the complete contents of the specified byte array
     * to this {@code ByteArrayOutputStream}.
     *
     * @apiNote
     * This method is equivalent to {@link #write(byte[],int,int)
     * write(b, 0, b.length)}.
     *
     * @param   b     the data.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @since   11
     */
    public void writeBytes(byte b[]) {
        write(b, 0, b.length);
    }

    /**
     * Writes the complete contents of this {@code ByteArrayOutputStream} to
     * the specified output stream argument, as if by calling the output
     * stream's write method using {@code out.write(buf, 0, count)}.
     *
     * @param   out   the output stream to which to write the data.
     * @throws  NullPointerException if {@code out} is {@code null}.
     * @throws  IOException if an I/O error occurs.
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the {@code count} field of this {@code ByteArrayOutputStream}
     * to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     java.io.ByteArrayInputStream#count
     */
    public synchronized void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return  the value of the {@code count} field, which is the number
     *          of valid bytes in this output stream.
     * @see     java.io.ByteArrayOutputStream#count
     */
    public synchronized int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string decoding bytes using the
     * platform's default character set. The length of the new {@code String}
     * is a function of the character set, and hence may not be equal to the
     * size of the buffer.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the default replacement string for the platform's
     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
     * class should be used when more control over the decoding process is
     * required.
     *
     * @return String decoded from the buffer's contents.
     * @since  1.1
     */
    public synchronized String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}.
     *
     * <p> This method is equivalent to {@code #toString(charset)} that takes a
     * {@link java.nio.charset.Charset charset}.
     *
     * <p> An invocation of this method of the form
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString("UTF-8")
     *      }
     * </pre>
     *
     * behaves in exactly the same way as the expression
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString(StandardCharsets.UTF_8)
     *      }
     * </pre>
     *
     *
     * @param  charsetName  the name of a supported
     *         {@link java.nio.charset.Charset charset}
     * @return String decoded from the buffer's contents.
     * @throws UnsupportedEncodingException
     *         If the named charset is not supported
     * @since  1.1
     */
    public synchronized String toString(String charsetName)
        throws UnsupportedEncodingException
    {
        return new String(buf, 0, count, charsetName);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the specified {@link java.nio.charset.Charset charset}. The length of the new
     * {@code String} is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param      charset  the {@linkplain java.nio.charset.Charset charset}
     *             to be used to decode the {@code bytes}
     * @return     String decoded from the buffer's contents.
     * @since      10
     */
    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }

    /**
     * Creates a newly allocated string. Its size is the current size of
     * the output stream and the valid contents of the buffer have been
     * copied into it. Each character <i>c</i> in the resulting string is
     * constructed from the corresponding element <i>b</i> in the byte
     * array such that:
     * <blockquote><pre>{@code
     *     c == (char)(((hibyte & 0xff) << 8) | (b & 0xff))
     * }</pre></blockquote>
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@link #toString(String charsetName)} or {@link #toString(Charset charset)}
     * method, which takes an encoding-name or charset argument,
     * or the {@code toString()} method, which uses the platform's default
     * character encoding.
     *
     * @param      hibyte    the high byte of each resulting Unicode character.
     * @return     the current contents of the output stream, as a string.
     * @see        java.io.ByteArrayOutputStream#size()
     * @see        java.io.ByteArrayOutputStream#toString(String)
     * @see        java.io.ByteArrayOutputStream#toString()
     */
    @Deprecated
    public synchronized String toString(int hibyte) {
        return new String(buf, hibyte, 0, count);
    }

    /**
     * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     */
    public void close() throws IOException {
    }


    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code (byte)0}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public byte getByte(int index) {
		return buf[index];
	}
	
}
