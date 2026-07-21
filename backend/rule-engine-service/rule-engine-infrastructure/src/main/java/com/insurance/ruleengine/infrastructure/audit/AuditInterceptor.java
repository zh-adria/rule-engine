package com.insurance.ruleengine.infrastructure.audit;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 审计拦截器 - 自动捕获请求的IP地址和User-Agent
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取客户端IP
        String ip = getClientIp(request);
        AuditContext.setIpAddress(ip);

        // 获取User-Agent
        String userAgent = request.getHeader("User-Agent");
        AuditContext.setUserAgent(userAgent);

        // Gateway writes X-Auth-Username after JWT validation. Keep X-Username only for older local calls.
        String username = request.getHeader("X-Auth-Username");
        if (username == null || username.isEmpty()) {
            username = request.getHeader("X-Username");
        }
        if (username != null && !username.isEmpty()) {
            AuditContext.setUsername(username);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        AuditContext.clear();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
