package com.insurance.ruleengine.infrastructure.drools;

import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DroolsRuleExecutionGateway implements RuleExecutionGateway {
    private final ConcurrentMap<String, KieBase> kieBaseCache = new ConcurrentHashMap<>();

    @Value("${rule-engine.execution.max-fired-rules:1000}")
    int maxFiredRules = 1000;

    @Value("${rule-engine.execution.timeout-ms:3000}")
    long timeoutMs = 3000;

    @Override
    public void validateDrl(String drlContent) {
        Results results = new KieHelper().addContent(drlContent, ResourceType.DRL).verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalArgumentException("invalid drl: " + results.getMessages(Message.Level.ERROR));
        }
    }

    @Override
    public ExecutionResult execute(RuleVersion version, ExecutionRequest request) {
        long startNanos = System.nanoTime();
        KieSession session = cachedKieBase(version).newKieSession();
        try {
            ExecutionResult result = new ExecutionResult();
            result.setTraceId(request.getTraceId());
            result.setRuleCode(version.getRuleCode());
            result.setVersion(version.getVersion());
            session.insert(request.getFacts());
            session.insert(result);
            int firedRules = fireRules(session);
            if (firedRules >= maxFiredRules) {
                throw new IllegalStateException("rule execution reached maximum fired rules: " + maxFiredRules);
            }
            result.setElapsedMs((System.nanoTime() - startNanos) / 1_000_000);
            return result;
        } finally {
            session.dispose();
        }
    }

    int cachedRuleCount() {
        return kieBaseCache.size();
    }

    private KieBase cachedKieBase(RuleVersion version) {
        return kieBaseCache.computeIfAbsent(cacheKey(version), key -> new KieHelper()
                .addContent(version.getDrlContent(), ResourceType.DRL)
                .build());
    }

    private int fireRules(KieSession session) {
        if (timeoutMs <= 0) {
            return session.fireAllRules(maxFiredRules);
        }
        FutureTask<Integer> task = new FutureTask<>(fireAllRulesTask(session));
        Thread worker = new Thread(task, "drools-rule-execution");
        worker.setDaemon(true);
        worker.start();
        try {
            return task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            session.halt();
            task.cancel(true);
            throw new IllegalStateException("rule execution timed out after " + timeoutMs + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("rule execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("rule execution failed", cause);
        }
    }

    private Callable<Integer> fireAllRulesTask(KieSession session) {
        return () -> session.fireAllRules(maxFiredRules);
    }

    private String cacheKey(RuleVersion version) {
        String contentFingerprint = version.getChecksum();
        if (contentFingerprint == null || contentFingerprint.isBlank()) {
            contentFingerprint = Integer.toHexString(Objects.hashCode(version.getDrlContent()));
        }
        return version.getRuleCode() + ":" + version.getVersion() + ":" + contentFingerprint;
    }
}
