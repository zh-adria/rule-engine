package com.insurance.ruleengine.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P2-4: rate limiter filter tests.
 */
class RateLimitFilterTest {

    @Test
    void allowsTrafficUnderLimit() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(true, 5, 60);
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
            assertEquals(200, resp.getStatus());
        }
    }

    @Test
    void blocksTrafficOverLimit() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(true, 3, 60);
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("1.2.3.5");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
        }
        // 4th request: OVER limit
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.5");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(429, resp.getStatus());
    }

    @Test
    void unlimitedWhenDisabled() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(false, 1, 60);
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("1.2.3.6");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
            assertEquals(200, resp.getStatus());
        }
    }
}
