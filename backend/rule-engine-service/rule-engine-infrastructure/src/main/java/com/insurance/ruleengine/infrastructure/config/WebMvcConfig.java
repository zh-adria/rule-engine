package com.insurance.ruleengine.infrastructure.config;

import com.insurance.ruleengine.infrastructure.audit.AuditInterceptor;
import com.insurance.ruleengine.infrastructure.rate.RateLimitInterceptor;
import com.insurance.ruleengine.infrastructure.security.ApprovalCallbackSecretInterceptor;
import com.insurance.ruleengine.infrastructure.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final PermissionInterceptor permissionInterceptor;
    private final ApprovalCallbackSecretInterceptor approvalCallbackSecretInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        AuditInterceptor auditInterceptor,
                        PermissionInterceptor permissionInterceptor,
                        ApprovalCallbackSecretInterceptor approvalCallbackSecretInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.auditInterceptor = auditInterceptor;
        this.permissionInterceptor = permissionInterceptor;
        this.approvalCallbackSecretInterceptor = approvalCallbackSecretInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 审计拦截器（先执行）
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        registry.addInterceptor(approvalCallbackSecretInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        // 限流拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
