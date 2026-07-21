package com.insurance.ruleengine.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RuleExecutionMetrics {

    private final Counter ruleExecutionCounter;
    private final Counter ruleHitCounter;
    private final Counter ruleTestCounter;
    private final Counter rulePublishCounter;
    private final Timer ruleExecutionTimer;

    public RuleExecutionMetrics(MeterRegistry meterRegistry) {
        this.ruleExecutionCounter = Counter.builder("rule.execution.total")
                .description("Total number of rule executions")
                .tag("type", "execute")
                .register(meterRegistry);

        this.ruleHitCounter = Counter.builder("rule.hit.total")
                .description("Total number of rule hits")
                .register(meterRegistry);

        this.ruleTestCounter = Counter.builder("rule.test.total")
                .description("Total number of rule tests")
                .register(meterRegistry);

        this.rulePublishCounter = Counter.builder("rule.publish.total")
                .description("Total number of rule publishes")
                .register(meterRegistry);

        this.ruleExecutionTimer = Timer.builder("rule.execution.duration")
                .description("Rule execution duration")
                .register(meterRegistry);
    }

    public void recordExecution(String ruleCode, int version, String decision, long elapsedMs) {
        ruleExecutionCounter.increment();
        ruleExecutionTimer.record(elapsedMs, TimeUnit.MILLISECONDS);
    }

    public void recordHit(String ruleCode, String hitRule) {
        ruleHitCounter.increment();
    }

    public void recordTest(String ruleCode, int version) {
        ruleTestCounter.increment();
    }

    public void recordPublish(String ruleCode, int version, int grayPercent) {
        rulePublishCounter.increment();
    }
}
