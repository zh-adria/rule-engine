package com.insurance.ruleengine.infrastructure.audit;

/**
 * 审计上下文 - 存储当前请求的审计信息
 */
public class AuditContext {

    private static final ThreadLocal<String> IP_ADDRESS = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void setIpAddress(String ipAddress) {
        IP_ADDRESS.set(ipAddress);
    }

    public static String getIpAddress() {
        return IP_ADDRESS.get();
    }

    public static void setUserAgent(String userAgent) {
        USER_AGENT.set(userAgent);
    }

    public static String getUserAgent() {
        return USER_AGENT.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        IP_ADDRESS.remove();
        USER_AGENT.remove();
        USERNAME.remove();
    }
}
