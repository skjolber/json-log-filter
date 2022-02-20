package com.github.skjolberg.jsonfilter.spring.logbook.servlet;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

public class LocalResponse extends HttpServletResponseWrapper implements HttpResponse {

    private final String protocolVersion;

    private Tee body;
    private Tee buffer;
    private boolean used; // point of no return, once we exposed our stream, we need to buffer

    public LocalResponse(final HttpServletResponse response, final String protocolVersion) {
        super(response);
        this.protocolVersion = protocolVersion;
    }

    @Override
    public Origin getOrigin() {
        return Origin.LOCAL;
    }

    @Override
    public String getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public HttpHeaders getHeaders() {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        for (final String header : getHeaderNames()) {
        	headers.put(header, new ArrayList<>(getHeaders(header)));
        }

		return HttpHeaders.of(headers );
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(getCharacterEncoding()).map(Charset::forName).orElse(UTF_8);
    }

    @Override
    public LocalResponse withBody() throws IOException {
        if (body == null) {
            bufferIfNecessary();
            this.body = buffer;
        }
        return this;
    }

    private void bufferIfNecessary() throws IOException {
        if (buffer == null) {
            this.buffer = new Tee(super.getOutputStream());
        }
    }

    @Override
    public LocalResponse withoutBody() {
        this.body = null;
        if (!used) {
            this.buffer = null;
        }
        return this;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (buffer == null) {
            return super.getOutputStream();
        } else {
            this.used = true;
            return buffer.getOutputStream();
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (buffer == null) {
            return super.getWriter();
        } else {
            this.used = true;
            return buffer.getWriter(this::getCharset);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        if (buffer != null) {
            buffer.flush();
        }
        super.flushBuffer();
    }

    @Override
    public byte[] getBody() {
        return body == null ? new byte[0] : body.getBytes();
    }

    private static class Tee {

        private final ByteArrayOutputStream branch;
        private final TeeServletOutputStream output;

        private PrintWriter writer;
        private byte[] bytes;

        private Tee(final ServletOutputStream original) {
            this.branch = new ByteArrayOutputStream();
            this.output = new TeeServletOutputStream(original, branch);
        }

        ServletOutputStream getOutputStream() {
            return output;
        }

        PrintWriter getWriter(final Supplier<Charset> charset) {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(output, charset.get()));
            }
            return writer;
        }

        void flush() throws IOException {
            if (writer == null) {
                output.flush();
            } else {
                writer.flush();
            }
        }

        byte[] getBytes() {
            if (bytes == null) {
                bytes = branch.toByteArray();
            }
            return bytes;
        }
    }

    private static class TeeServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream original;
        private final OutputStream branch;

        public TeeServletOutputStream(ServletOutputStream original, ByteArrayOutputStream branch) {
			this.original = original;
			this.branch = branch;
		}

		@Override
        public void write(final int b) throws IOException {
            original.write(b);
            branch.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            original.write(b, off, len);
            branch.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            original.flush();
            branch.flush();
        }

        @Override
        public void close() throws IOException {
            original.close();
            branch.close();
        }

        @Override
        public boolean isReady() {
            return original.isReady();
        }

        @Override
        public void setWriteListener(final WriteListener listener) {
            original.setWriteListener(listener);
        }

    }
}
