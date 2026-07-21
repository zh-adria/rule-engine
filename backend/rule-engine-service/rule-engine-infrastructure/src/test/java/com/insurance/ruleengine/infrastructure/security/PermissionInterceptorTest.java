package com.insurance.ruleengine.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

class PermissionInterceptorTest {

    private final PermissionInterceptor interceptor = new PermissionInterceptor();

    @Test
    void allowsReadEndpointWhenPermissionHeaderContainsRuleRead() throws Exception {
        HttpServletRequest request = request("GET", "/api/v1/rules", "RULE_READ,AUDIT_READ");

        boolean allowed = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertTrue(allowed);
    }

    @Test
    void rejectsWriteEndpointWhenRequiredPermissionIsMissing() throws Exception {
        HttpServletRequest request = request("POST", "/api/v1/rules", "RULE_READ");
        CapturingResponse response = new CapturingResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsApprovalCallbackWithoutUserPermission() throws Exception {
        HttpServletRequest request = request(
                "POST",
                "/api/v1/rules/CI_UW_001/versions/2/approval-callback",
                null);

        boolean allowed = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

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
