package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.app.RuleEngineFacadeImpl;
import com.insurance.ruleengine.domain.model.DecisionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * P3-4: Metrics aggregation endpoint for Dashboard.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "监控指标", description = "规则引擎执行监控数据")
public class MetricsController {
    private final RuleEngineFacadeImpl facade;

    public MetricsController(RuleEngineFacadeImpl facade) {
        this.facade = facade;
    }

    @Operation(summary = "规则引擎执行指标聚合")
    @GetMapping("/execution")
    public Map<String, Object> executionMetrics(@RequestParam(required = false) String ruleCode) {
        Map<String, Object> result = new HashMap<>();
        try {
            var executions = facade.listExecutions(ruleCode);
            result.put("totalExecutions", executions.size());
            Map<String, Long> decisionCounts = executions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            e -> e.getDecision() != null ? e.getDecision() : "UNKNOWN",
                            java.util.stream.Collectors.counting()));
            result.put("decisionDistribution", decisionCounts);
            double avgMs = executions.stream()
                    .mapToLong(e -> e.getElapsedMs() != null ? e.getElapsedMs() : 0)
                    .average().orElse(0);
            result.put("avgElapsedMs", Math.round(avgMs));
        } catch (Exception e) {
            result.put("totalExecutions", 0);
            result.put("decisionDistribution", Map.of());
            result.put("avgElapsedMs", 0);
        }
        return result;
    }
}
