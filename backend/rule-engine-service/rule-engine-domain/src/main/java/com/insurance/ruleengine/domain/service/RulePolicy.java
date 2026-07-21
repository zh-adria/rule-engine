package com.insurance.ruleengine.domain.service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RulePolicy {
    private static final Pattern SAFE_RULE_CODE = Pattern.compile("^[A-Z0-9_\\-]{3,64}$");

    public void validateRuleCode(String ruleCode) {
        if (ruleCode == null || !SAFE_RULE_CODE.matcher(ruleCode).matches()) {
            throw new IllegalArgumentException("ruleCode only supports A-Z, 0-9, underscore and hyphen");
        }
    }

    private static final Set<String> BLOCKED_VALUES = Set.of(
            "<script", "</script>", "javascript:", "onerror=", "onload=",
            ";--", "drop table", "insert into", "delete from", "update set"
    );

    public void validateFacts(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) {
            throw new IllegalArgumentException("facts can not be empty");
        }
        facts.forEach((key, value) -> {
            if (key == null || key.contains("$") || key.contains(".") || key.length() > 64) {
                throw new IllegalArgumentException("unsafe fact key: " + key);
            }
            if (value instanceof String s) {
                String lower = s.toLowerCase();
                for (String blocked : BLOCKED_VALUES) {
                    if (lower.contains(blocked)) {
                        throw new IllegalArgumentException("unsafe fact value for key " + key + ": contains blocked content");
                    }
                }
            }
        });
    }
}

