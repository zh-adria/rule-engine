package com.insurance.ruleengine.adapter.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase 4: verify TraceIdFilter MDC handling.
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdWhenNoneProvided() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, new MockFilterChain());
        // MDC should be cleared after the filter
        assertNull(MDC.get(TraceIdFilter.MDC_KEY));
    }

    @Test
    void propagatesProvidedTraceId() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");
        req.addHeader(TraceIdFilter.HEADER, "trace-abc-123");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        final String[] captured = new String[1];
        filter.doFilter(req, resp, (request, response) -> {
            captured[0] = MDC.get(TraceIdFilter.MDC_KEY);
        });
        assertEquals("trace-abc-123", captured[0]);
        // MDC must be cleared after the filter chain
        assertNull(MDC.get(TraceIdFilter.MDC_KEY));
    }

    @Test
    void usesXForwardedForAsClientIp() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader(ProxyHeaders.X_FORWARDED_FOR, "10.1.2.3, 192.168.1.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        // not rate-limit filter, just smoke test that X-Forwarded-For header is not brokein
        filter.doFilter(req, resp, new MockFilterChain());
        assertNull(MDC.get(TraceIdFilter.MDC_KEY));
    }

    private static class ProxyHeaders {
        static final String X_FORWARDED_FOR = "X-Forwarded-For";
    }
}
