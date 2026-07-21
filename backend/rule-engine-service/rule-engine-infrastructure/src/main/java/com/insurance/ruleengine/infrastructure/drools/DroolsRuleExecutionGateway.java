package com.insurance.ruleengine.infrastructure.drools;

import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.RuleVersion;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DroolsRuleExecutionGateway implements RuleExecutionGateway {
    // P2-3: LRU cache — LinkedHashMap access-order=true, manual size guard
    private final LinkedHashMap<String, KieBase> kieBaseCache = new LinkedHashMap<>(256, 0.75f, true);

    @Value("${rule-engine.cache.kiebase.max-size:200}")
    int maxCacheSize = 200;

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
            // P1-4: track which rules fired
            session.addEventListener(new HitRuleListener(result));
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

    /**
     * P1-4: captures fired rule names into ExecutionResult.hitRules.
     */
    private static class HitRuleListener extends DefaultAgendaEventListener {
        private final ExecutionResult result;

        HitRuleListener(ExecutionResult result) {
            this.result = result;
        }

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            result.getHitRules().add(event.getMatch().getRule().getName());
        }
    }

    int cachedRuleCount() {
        synchronized (kieBaseCache) {
            return kieBaseCache.size();
        }
    }

    private KieBase cachedKieBase(RuleVersion version) {
        String key = cacheKey(version);
        synchronized (kieBaseCache) {
            KieBase existing = kieBaseCache.get(key);
            if (existing != null) {
                return existing;
            }
            KieBase built = new KieHelper()
                    .addContent(version.getDrlContent(), ResourceType.DRL)
                    .build();
            kieBaseCache.put(key, built);
            trimCache();
            return built;
        }
    }

    private void trimCache() {
        int limit = Math.max(1, maxCacheSize);
        Iterator<Map.Entry<String, KieBase>> iterator = kieBaseCache.entrySet().iterator();
        while (kieBaseCache.size() > limit && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
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
