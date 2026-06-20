package com.insurance.ruleengine.domain.service;

import java.util.Map;
import java.util.regex.Pattern;

public class RulePolicy {
    private static final Pattern SAFE_RULE_CODE = Pattern.compile("^[A-Z0-9_\\-]{3,64}$");

    public void validateRuleCode(String ruleCode) {
        if (ruleCode == null || !SAFE_RULE_CODE.matcher(ruleCode).matches()) {
            throw new IllegalArgumentException("ruleCode only supports A-Z, 0-9, underscore and hyphen");
        }
    }

    public void validateFacts(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) {
            throw new IllegalArgumentException("facts can not be empty");
        }
        facts.keySet().forEach(key -> {
            if (key == null || key.contains("$") || key.contains(".") || key.length() > 64) {
                throw new IllegalArgumentException("unsafe fact key: " + key);
            }
        });
    }
}

