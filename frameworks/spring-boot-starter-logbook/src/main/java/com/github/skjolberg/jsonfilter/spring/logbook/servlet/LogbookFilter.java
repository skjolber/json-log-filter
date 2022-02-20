package com.github.skjolberg.jsonfilter.spring.logbook.servlet;

import org.apiguardian.api.API;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.RequestWritingStage;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.logbook.Logbook.ResponseWritingStage;
import org.zalando.logbook.Strategy;

import javax.annotation.Nullable;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.servlet.DispatcherType.ASYNC;

public final class LogbookFilter implements HttpFilter {

    public static final String responseProcessingStageName = ResponseProcessingStage.class.getName() + "-" + UUID.randomUUID();
    public static final String responseWritingStageSynchronizationName = ResponseWritingStage.class.getName() + "-Synchronization-"+ UUID.randomUUID();

    public static final String STAGE = ResponseProcessingStage.class.getName();

    private final Logbook logbook;
    private final Strategy strategy;

    public LogbookFilter() {
        this(Logbook.create());
    }

    public LogbookFilter(final Logbook logbook) {
        this(logbook, null);
    }

    public LogbookFilter(final Logbook logbook, @Nullable final Strategy strategy) {
        this.logbook = logbook;
        this.strategy = strategy;
    }
    @Override
    public void doFilter(final HttpServletRequest request, final HttpServletResponse httpResponse,
                         final FilterChain chain) throws ServletException, IOException {

        //final RemoteRequest request = new RemoteRequest(httpRequest);
        final LocalResponse response = new LocalResponse(httpResponse, request.getProtocol());

        chain.doFilter(request, (ServletResponse) response.withBody());

        if (request.isAsyncStarted()) {
            return;
        }
        final ResponseProcessingStage processing = (ResponseProcessingStage) request.getAttribute(responseProcessingStageName);

        final ResponseWritingStage writing = processing.process(response);
        
        request.setAttribute(responseWritingStageSynchronizationName, new AtomicBoolean(false));

        write(request, response, writing);
    }

    private void write(HttpServletRequest request, LocalResponse response, ResponseWritingStage writing) throws IOException {
        final AtomicBoolean attribute = (AtomicBoolean) request.getAttribute(responseWritingStageSynchronizationName);
        if (!attribute.getAndSet(true)) {
            response.flushBuffer();
            writing.write();
        }
    }

}
