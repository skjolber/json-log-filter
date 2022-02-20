package com.github.skjolberg.jsonfilter.spring.logbook.servlet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.activation.MimeType;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;
import org.zalando.logbook.servlet.FormRequestMode;

public class RemoteRequest extends HttpServletRequestWrapper implements HttpRequest {

	protected final FormRequestMode formRequestMode = FormRequestMode.fromProperties();

    protected byte[] body;
    protected byte[] buffered;

    public RemoteRequest(final HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getProtocolVersion() {
        return getProtocol();
    }

    @Override
    public Origin getOrigin() {
        return Origin.REMOTE;
    }

    @Override
    public String getRemote() {
        return getRemoteAddr();
    }

    @Override
    public String getHost() {
        return getServerName();
    }

    @Override
    public Optional<Integer> getPort() {
        return Optional.of(getServerPort());
    }

    @Override
    public String getPath() {
        return getRequestURI();
    }

    @Override
    public String getQuery() {
        return Optional.ofNullable(getQueryString()).orElse("");
    }

    @Override
    public HttpHeaders getHeaders() {
        
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        final Enumeration<String> names = getHeaderNames();

        while (names.hasMoreElements()) {
            final String name = names.nextElement();

            headers.put(name, list(getHeaders(name)));
        }

		return HttpHeaders.of(headers );
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(getCharacterEncoding()).map(Charset::forName).orElse(UTF_8);
    }

    @Override
    public HttpRequest withBody() throws IOException {
        if (body == null) {
            bufferIfNecessary();
            this.body = buffered;
        }

        return this;
    }

    private void bufferIfNecessary() throws IOException {
        if (buffered == null) {
            if (isFormRequest()) {
                switch (formRequestMode) {
                    case PARAMETER:
                        this.buffered = reconstructBodyFromParameters();
                        return;
                    case OFF:
                        this.buffered = new byte[0];
                        return;
                    default:
                        break;
                }
            }

            this.buffered = ByteStreams.toByteArray(super.getInputStream());
        }
    }

    @Override
    public HttpRequest withoutBody() {
        this.body = null;
        return this;
    }

    private boolean isFormRequest() {
        return Optional.ofNullable(getContentType())
                .flatMap(MimeTypes::parse)
                .filter(this::isFormRequest)
                .isPresent();
    }

    private boolean isFormRequest(final MimeType contentType) {
        return "application".equals(contentType.getPrimaryType()) &&
                "x-www-form-urlencoded".equals(contentType.getSubType());
    }

    private byte[] reconstructBodyFromParameters() {
        return getParameterMap().entrySet().stream()
                .flatMap(entry -> Arrays.stream(entry.getValue())
                        .map(value -> encode(entry.getKey()) + "=" + encode(value)))
                .collect(joining("&"))
                .getBytes(UTF_8);
    }

    private static String encode(final String s) {
        return encode(s, "UTF-8");
    }

    static String encode(final String s, final String charset) {
        try {
			return URLEncoder.encode(s, charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return buffered == null ?
                super.getInputStream() :
                new ServletInputStreamAdapter(new ByteArrayInputStream(buffered));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharset()));
    }

    @Override
    public byte[] getBody() {
        return body == null ? new byte[0] : body;
    }
}
