package com.insurance.ruleengine.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalCallbackSecretInterceptorTest {

    @Test
    void rejectsApprovalCallbackWhenSecretIsMissing() throws Exception {
        ApprovalCallbackSecretInterceptor interceptor =
                new ApprovalCallbackSecretInterceptor(true, "callback-secret");
        CapturingResponse response = new CapturingResponse();

        boolean allowed = interceptor.preHandle(callbackRequest(null), response, new Object());

        assertFalse(allowed);
        assertTrue(response.status == 401);
    }

    @Test
    void allowsApprovalCallbackWhenSecretMatches() throws Exception {
        ApprovalCallbackSecretInterceptor interceptor =
                new ApprovalCallbackSecretInterceptor(true, "callback-secret");

        boolean allowed = interceptor.preHandle(callbackRequest("callback-secret"), mock(HttpServletResponse.class), new Object());

        assertTrue(allowed);
    }

    @Test
    void ignoresNonCallbackPaths() throws Exception {
        ApprovalCallbackSecretInterceptor interceptor =
                new ApprovalCallbackSecretInterceptor(true, "callback-secret");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/rules");

        boolean allowed = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertTrue(allowed);
    }

    private HttpServletRequest callbackRequest(String secret) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/rules/RULE_001/versions/2/approval-callback");
        when(request.getHeader("X-Approval-Callback-Secret")).thenReturn(secret);
        return request;
    }

    private static class CapturingResponse extends javax.servlet.http.HttpServletResponseWrapper {
        private int status = 200;
        private final StringWriter body = new StringWriter();

        CapturingResponse() {
            super(mock(HttpServletResponse.class));
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(body);
        }
    }
}
