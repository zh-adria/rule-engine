package com.insurance.ruleengine.infrastructure.config;

import com.insurance.ruleengine.domain.service.RuleTestAssertionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RuleEngineProperties.class)
public class InfrastructureConfig {
    @Bean
    public RuleTestAssertionService ruleTestAssertionService() {
        return new RuleTestAssertionService();
    }
}
