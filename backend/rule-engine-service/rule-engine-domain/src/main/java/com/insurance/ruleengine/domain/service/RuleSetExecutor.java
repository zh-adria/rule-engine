package com.insurance.ruleengine.domain.service;

import com.insurance.ruleengine.domain.gateway.RuleExecutionGateway;
import com.insurance.ruleengine.domain.gateway.RuleGateway;
import com.insurance.ruleengine.domain.model.DecisionType;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.ExecutionMode;
import com.insurance.ruleengine.domain.model.RuleDefinition;
import com.insurance.ruleengine.domain.model.RuleSet;
import com.insurance.ruleengine.domain.model.RuleSetStep;
import com.insurance.ruleengine.domain.model.RuleStatus;
import com.insurance.ruleengine.domain.model.RuleVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * P1-2: executes a RuleSet either serial or parallel based on each step's mode.
 * Used by RuleEngineFacadeImpl.executeRuleSet().
 */
public class RuleSetExecutor {

    private final RuleGateway ruleGateway;
    private final RuleExecutionGateway executionGateway;
    private final BiFunction<RuleDefinition, RuleVersion, RuleVersion> cryptoResolver;

    public RuleSetExecutor(RuleGateway ruleGateway, RuleExecutionGateway executionGateway,
                           BiFunction<RuleDefinition, RuleVersion, RuleVersion> cryptoResolver) {
        this.ruleGateway = ruleGateway;
        this.executionGateway = executionGateway;
        this.cryptoResolver = cryptoResolver;
    }

    public static class StepOutput {
        public final int stepOrder;
        public final String ruleCode;
        public DecisionType decision = DecisionType.ACCEPT;
        public List<String> hitRules = new ArrayList<>();
        public Map<String, Object> outputs = new LinkedHashMap<>();
        public boolean skipped = false;
        public boolean error = false;

        public StepOutput(int stepOrder, String ruleCode) {
            this.stepOrder = stepOrder;
            this.ruleCode = ruleCode;
        }
    }

    public static class RuleSetOutput {
        public List<StepOutput> stepOutputs = new ArrayList<>();
        public DecisionType finalDecision = DecisionType.ACCEPT;
        public List<String> allHitRules = new ArrayList<>();
        public Map<String, Object> mergedOutputs = new LinkedHashMap<>();
    }

    public RuleSetOutput execute(RuleSet ruleSet, java.util.Map<String, Object> facts,
                                 String scenario, String operator, String suppliedTraceId) {
        List<RuleSetStep> steps = ruleSet.getSteps() == null ? List.of() : ruleSet.getSteps();
        RuleSetOutput out = new RuleSetOutput();

        // Partition into serial and parallel runs: adjacent PARALLEL steps form a batch,
        // SERIAL steps run alone. We walk the list.
        int idx = 0;
        boolean stopped = false;
        while (idx < steps.size() && !stopped) {
            RuleSetStep step = steps.get(idx);
            if (step.getMode() == ExecutionMode.SERIAL || idx == steps.size() - 1 || steps.get(idx + 1).getMode() == ExecutionMode.SERIAL) {
                // run single step
                if (stopped) {
                    StepOutput skipped = new StepOutput(step.getStepOrder(), step.getRuleCode());
                    skipped.skipped = true;
                    out.stepOutputs.add(skipped);
                    idx++;
                    continue;
                }
                StepOutput so = executeSingle(step, facts, scenario, operator, suppliedTraceId);
                out.stepOutputs.add(so);
                accumulate(out, so);
                if (step.isStopOnDecline() && so.decision == DecisionType.DECLINE) {
                    stopped = true;
                }
                idx++;
            } else {
                // gather PARALLEL batch
                List<RuleSetStep> batch = new ArrayList<>();
                while (idx < steps.size() && steps.get(idx).getMode() == ExecutionMode.PARALLEL && !stopped) {
                    batch.add(steps.get(idx));
                    idx++;
                }
                List<StepOutput> batchResults = executeBatchParallel(batch, facts, scenario, operator, suppliedTraceId);
                for (int b = 0; b < batchResults.size(); b++) {
                    StepOutput so = batchResults.get(b);
                    RuleSetStep stp = batch.get(b);
                    out.stepOutputs.add(so);
                    if (!so.skipped) {
                        accumulate(out, so);
                        if (stp.isStopOnDecline() && so.decision == DecisionType.DECLINE) {
                            stopped = true;
                        }
                    }
                }
            }
        }
        // mark remaining as skipped
        while (idx < steps.size()) {
            StepOutput skipped = new StepOutput(steps.get(idx).getStepOrder(), steps.get(idx).getRuleCode());
            skipped.skipped = true;
            out.stepOutputs.add(skipped);
            idx++;
        }
        return out;
    }

    private List<StepOutput> executeBatchParallel(List<RuleSetStep> batch, java.util.Map<String, Object> facts,
                                                   String scenario, String operator, String suppliedTraceId) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, batch.size()));
        try {
            List<Callable<StepOutput>> tasks = new ArrayList<>();
            for (RuleSetStep step : batch) {
                tasks.add(() -> executeSingle(step, facts, scenario, operator, suppliedTraceId));
            }
            List<Future<StepOutput>> futures = pool.invokeAll(tasks);
            List<StepOutput> results = new ArrayList<>();
            for (Future<StepOutput> f : futures) {
                try {
                    results.add(f.get());
                } catch (Exception e) {
                    StepOutput err = new StepOutput(-1, "?");
                    err.error = true;
                    err.decision = DecisionType.MANUAL_REVIEW;
                    results.add(err);
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            List<StepOutput> errs = new ArrayList<>();
            for (RuleSetStep step : batch) {
                StepOutput err = new StepOutput(step.getStepOrder(), step.getRuleCode());
                err.error = true;
                errs.add(err);
            }
            return errs;
        } finally {
            pool.shutdownNow();
        }
    }

    private StepOutput executeSingle(RuleSetStep step, java.util.Map<String, Object> facts,
                                      String scenario, String operator, String suppliedTraceId) {
        StepOutput so = new StepOutput(step.getStepOrder(), step.getRuleCode());
        try {
            RuleDefinition rule = ruleGateway.findRule(step.getRuleCode())
                    .orElseThrow(() -> new IllegalStateException("rule not found: " + step.getRuleCode()));
            if (rule.isArchived()) {
                throw new IllegalStateException("rule archived: " + step.getRuleCode());
            }
            RuleVersion version;
            if (step.getRuleVersion() != null) {
                version = ruleGateway.findVersion(step.getRuleCode(), step.getRuleVersion())
                        .orElseThrow(() -> new IllegalStateException("version not found"));
            } else {
                version = ruleGateway.findCurrentVersion(step.getRuleCode())
                        .orElseThrow(() -> new IllegalStateException("no published version"));
            }
            RuleVersion executable = cryptoResolver.apply(rule, version);
            ExecutionRequest request = new ExecutionRequest();
            request.setRuleCode(step.getRuleCode());
            request.setVersion(version.getVersion());
            request.setScenario(scenario);
            request.setFacts(facts);
            request.setOperator(operator);
            request.setTraceId(suppliedTraceId == null || suppliedTraceId.isBlank()
                    ? UUID.randomUUID().toString() : suppliedTraceId);
            ExecutionResult r = executionGateway.execute(executable, request);
            so.decision = r.getDecision();
            so.hitRules = new ArrayList<>(r.getHitRules());
            so.outputs = new LinkedHashMap<>(r.getOutputs());
            return so;
        } catch (Exception e) {
            so.error = true;
            so.decision = DecisionType.MANUAL_REVIEW;
            return so;
        }
    }

    private void accumulate(RuleSetOutput out, StepOutput so) {
        out.allHitRules.addAll(so.hitRules);
        out.mergedOutputs.putAll(so.outputs);
        // escalate: worst decision wins per ordinal
        if (so.decision != null && so.decision.ordinal() > out.finalDecision.ordinal()) {
            out.finalDecision = so.decision;
        }
    }
}
