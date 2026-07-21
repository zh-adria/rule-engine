package com.insurance.ruleengine.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 4: rate limiter edge cases.
 */
class RateLimitFilterEdgeCaseTest {

    @Test
    void differentIpsHaveIndependentCounters() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(true, 2, 60);
        // exhaust limit for IP A
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("1.1.1.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
            assertEquals(200, resp.getStatus());
        }
        // 3rd request from IP A: blocked
        MockHttpServletRequest reqA3 = new MockHttpServletRequest();
        reqA3.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse respA3 = new MockHttpServletResponse();
        filter.doFilter(reqA3, respA3, new MockFilterChain());
        assertEquals(429, respA3.getStatus());

        // IP B still has full quota
        MockHttpServletRequest reqB = new MockHttpServletRequest();
        reqB.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse respB = new MockHttpServletResponse();
        filter.doFilter(reqB, respB, new MockFilterChain());
        assertEquals(200, respB.getStatus());
    }

    @Test
    void xForwardedForUsedAsClientIp() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 60);
        // first request via proxy
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRemoteAddr("10.0.0.1");
        req1.addHeader("X-Forwarded-For", "99.99.99.99, 10.0.0.1");
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        filter.doFilter(req1, resp1, new MockFilterChain());
        assertEquals(200, resp1.getStatus());

        // second request from same client IP (via proxy): blocked
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("10.0.0.2");
        req2.addHeader("X-Forwarded-For", "99.99.99.99");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        filter.doFilter(req2, resp2, new MockFilterChain());
        assertEquals(429, resp2.getStatus());
    }
}
