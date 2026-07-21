package com.insurance.approval.adapter;

import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalPermissionInterceptorTest {

    private final ApprovalPermissionInterceptor interceptor = new ApprovalPermissionInterceptor();

    @Test
    void allowsApprovalListWithApprovalRead() throws Exception {
        boolean allowed = interceptor.preHandle(
                request("GET", "/api/v1/approvals", "APPROVAL_READ"),
                mock(HttpServletResponse.class),
                new Object());

        assertTrue(allowed);
    }

    @Test
    void rejectsApproveWhenRuleApproveIsMissing() throws Exception {
        CapturingResponse response = new CapturingResponse();

        boolean allowed = interceptor.preHandle(
                request("POST", "/api/v1/approvals/9/approve", "APPROVAL_READ"),
                response,
                new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsApproveWithRuleApprove() throws Exception {
        boolean allowed = interceptor.preHandle(
                request("POST", "/api/v1/approvals/9/approve", "RULE_APPROVE"),
                mock(HttpServletResponse.class),
                new Object());

        assertTrue(allowed);
    }

    private HttpServletRequest request(String method, String path, String permissions) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getHeader("X-Auth-Permissions")).thenReturn(permissions);
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
        public int getStatus() {
            return status;
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(body);
        }
    }
}
