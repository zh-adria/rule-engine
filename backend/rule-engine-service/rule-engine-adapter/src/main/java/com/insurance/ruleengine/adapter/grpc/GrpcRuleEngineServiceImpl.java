package com.insurance.ruleengine.adapter.grpc;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.domain.model.ExecutionRequest;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.grpc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * P3-external: gRPC service implementation.
 * Bridges gRPC calls to RuleEngineFacade.
 * Requires grpc-netty + protobuf runtime on classpath to start.
 */
@Service
public class GrpcRuleEngineServiceImpl extends RuleEngineExternalGrpc.RuleEngineExternalImplBase {

    private final RuleEngineFacade facade;

    public GrpcRuleEngineServiceImpl(RuleEngineFacade facade) {
        this.facade = facade;
    }

    @Override
    public void executeRule(ExecuteRuleRequest req, StreamObserver<ExecuteRuleResponse> resp) {
        try {
            ExecuteRuleCmd cmd = new ExecuteRuleCmd();
            cmd.setRuleCode(req.getRuleCode());
            cmd.setVersion(req.getVersion());
            cmd.setScenario(req.getScenario());
            cmd.setFacts(parseFacts(req.getFactsJson()));
            cmd.setOperator(req.getOperator());
            RuleExecutionResultDTO result = facade.execute(cmd);
            ExecuteRuleResponse reply = ExecuteRuleResponse.newBuilder()
                    .setTraceId(result.getTraceId() != null ? result.getTraceId() : "")
                    .setRuleCode(result.getRuleCode())
                    .setVersion(result.getVersion())
                    .setDecision(result.getDecision() != null ? result.getDecision() : "UNKNOWN")
                    .addAllHitRules(result.getHitRules() != null ? result.getHitRules() : List.of())
                    .setOutputsJson(result.getOutputs() != null ? result.getOutputs().toString() : "{}")
                    .setElapsedMs(result.getElapsedMs())
                    .build();
            resp.onNext(reply);
            resp.onCompleted();
        } catch (Exception e) {
            ExecuteRuleResponse error = ExecuteRuleResponse.newBuilder()
                    .setTraceId(req.getTraceId())
                    .setRuleCode(req.getRuleCode())
                    .setVersion(req.getVersion())
                    .setError(e.getMessage())
                    .build();
            resp.onNext(error);
            resp.onCompleted();
        }
    }

    @Override
    public void testRule(TestRuleRequest req, StreamObserver<TestRuleResponse> resp) {
        try {
            ExecuteRuleCmd cmd = new ExecuteRuleCmd();
            cmd.setRuleCode(req.getRuleCode());
            cmd.setVersion(req.getVersion());
            cmd.setScenario(req.getScenario());
            cmd.setFacts(parseFacts(req.getFactsJson()));
            cmd.setOperator(req.getOperator());
            RuleExecutionResultDTO result = facade.testRule(req.getRuleCode(), cmd);
            TestRuleResponse reply = TestRuleResponse.newBuilder()
                    .setTraceId(result.getTraceId() != null ? result.getTraceId() : "")
                    .setRuleCode(result.getRuleCode())
                    .setVersion(result.getVersion())
                    .setDecision(result.getDecision() != null ? result.getDecision() : "UNKNOWN")
                    .addAllHitRules(result.getHitRules() != null ? result.getHitRules() : List.of())
                    .setOutputsJson(result.getOutputs() != null ? result.getOutputs().toString() : "{}")
                    .setElapsedMs(result.getElapsedMs())
                    .build();
            resp.onNext(reply);
            resp.onCompleted();
        } catch (Exception e) {
            TestRuleResponse error = TestRuleResponse.newBuilder()
                    .setTraceId(req.getTraceId())
                    .setError(e.getMessage())
                    .build();
            resp.onNext(error);
            resp.onCompleted();
        }
    }

    @Override
    public void listRules(ListRulesRequest req, StreamObserver<ListRulesResponse> resp) {
        try {
            List<RuleDTO> rules = facade.listRules(req.getCategory(), req.getBusinessLine(), null, req.getKeyword());
            ListRulesResponse reply = ListRulesResponse.newBuilder()
                    .addAllRules(rules.stream().map(r -> RuleInfo.newBuilder()
                            .setRuleCode(r.getRuleCode())
                            .setRuleName(r.getRuleName())
                            .setCategory(r.getCategory())
                            .setBusinessLine(r.getBusinessLine())
                            .setCurrentVersion(r.getCurrentVersion())
                            .setStatus(r.getStatus() != null ? r.getStatus() : "UNKNOWN")
                            .setOwner(r.getOwner())
                            .build()).toList())
                    .setTotal(rules.size())
                    .build();
            resp.onNext(reply);
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void getRule(GetRuleRequest req, StreamObserver<GetRuleResponse> resp) {
        try {
            RuleDTO rule = facade.getRule(req.getRuleCode());
            GetRuleResponse reply = GetRuleResponse.newBuilder()
                    .setRuleCode(rule.getRuleCode())
                    .setRuleName(rule.getRuleName())
                    .setCategory(rule.getCategory())
                    .setBusinessLine(rule.getBusinessLine())
                    .setDescription(rule.getDescription() != null ? rule.getDescription() : "")
                    .setCurrentVersion(rule.getCurrentVersion())
                    .setStatus(rule.getStatus() != null ? rule.getStatus() : "UNKNOWN")
                    .setOwner(rule.getOwner())
                    .setSensitive(rule.isSensitive())
                    .build();
            resp.onNext(reply);
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void listVersions(ListVersionsRequest req, StreamObserver<ListVersionsResponse> resp) {
        try {
            List<com.insurance.ruleengine.client.dto.RuleVersionDTO> versions = facade.listVersions(req.getRuleCode());
            ListVersionsResponse reply = ListVersionsResponse.newBuilder()
                    .setRuleCode(req.getRuleCode())
                    .addAllVersions(versions.stream().map(v -> VersionInfo.newBuilder()
                            .setVersion(v.getVersion())
                            .setStatus(v.getStatus() != null ? v.getStatus() : "UNKNOWN")
                            .setCreatedBy(v.getCreatedBy() != null ? v.getCreatedBy() : "")
                            .setPublishedAt(v.getPublishedAt() != null ? v.getPublishedAt().toString() : "")
                            .build()).toList())
                    .build();
            resp.onNext(reply);
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFacts(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
