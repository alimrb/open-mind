package com.openmind.hagap.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID propagation — the foundation of request tracing across the entire system.
 *
 * <p>Why a servlet Filter (not a Spring Interceptor): Filters execute before Spring's DispatcherServlet,
 * so the correlation ID is available even for requests that fail during Spring's own processing
 * (e.g., deserialization errors, security filter chain). {@code HIGHEST_PRECEDENCE} ensures this
 * runs before all other filters — the ID must be set before anything logs.
 *
 * <p>Critical detail: the {@code finally} block removes MDC entries. Servlet containers (Tomcat)
 * reuse threads from a pool — without cleanup, Thread A's correlation ID would leak into Thread B's
 * next request. This is the #1 MDC bug in production Spring apps and causes log entries to be
 * attributed to the wrong request. Also removes workspaceId (set downstream by OrchestratorAgent).
 *
 * <p>The response header echo ({@code X-Correlation-Id}) enables client-side debugging:
 * "my request failed, the correlation ID was X" → grep logs for X → full request trace.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_WORKSPACE_ID = "workspaceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_WORKSPACE_ID);
        }
    }
}
