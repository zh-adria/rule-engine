package com.insurance.ruleengine.sdk;

import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class RuleEngineClient {
    private final WebClient webClient;
    private final Duration timeout;

    public RuleEngineClient(String baseUrl, String apiKey, Duration timeout) {
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(10);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-Api-Key", apiKey);
        }
        this.webClient = builder.build();
    }

    public Mono<RuleExecutionResultDTO> execute(String ruleCode, Map<String, Object> facts, String scenario, String operator) {
        ExecuteRuleCmd cmd = new ExecuteRuleCmd();
        cmd.setRuleCode(ruleCode);
        cmd.setScenario(scenario);
        cmd.setFacts(facts);
        cmd.setOperator(operator);
        return webClient.post()
                .uri("/rule-engine/api/v1/rules/execute")
                .body(Mono.just(cmd), ExecuteRuleCmd.class)
                .retrieve()
                .bodyToMono(RuleExecutionResultDTO.class)
                .timeout(timeout);
    }

    public Mono<RuleExecutionResultDTO> test(String ruleCode, int version, Map<String, Object> facts, String scenario, String operator) {
        ExecuteRuleCmd cmd = new ExecuteRuleCmd();
        cmd.setRuleCode(ruleCode);
        cmd.setVersion(version);
        cmd.setScenario(scenario);
        cmd.setFacts(facts);
        cmd.setOperator(operator);
        return webClient.post()
                .uri("/rule-engine/api/v1/rules/{ruleCode}/test", ruleCode)
                .body(Mono.just(cmd), ExecuteRuleCmd.class)
                .retrieve()
                .bodyToMono(RuleExecutionResultDTO.class)
                .timeout(timeout);
    }

    public Mono<List<RuleDTO>> listRules(String category, String businessLine) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/rule-engine/api/v1/rules")
                        .queryParamIfPresent("category", category != null ? java.util.Optional.of(category) : java.util.Optional.empty())
                        .queryParamIfPresent("businessLine", businessLine != null ? java.util.Optional.of(businessLine) : java.util.Optional.empty())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RuleDTO>>() {})
                .timeout(timeout);
    }

    public Mono<RuleDTO> getRule(String ruleCode) {
        return webClient.get()
                .uri("/rule-engine/api/v1/rules/{ruleCode}", ruleCode)
                .retrieve()
                .bodyToMono(RuleDTO.class)
                .timeout(timeout);
    }

    public Mono<List<RuleVersionDTO>> listVersions(String ruleCode) {
        return webClient.get()
                .uri("/rule-engine/api/v1/rules/{ruleCode}/versions", ruleCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RuleVersionDTO>>() {})
                .timeout(timeout);
    }

    public Mono<String> health() {
        return webClient.get()
                .uri("/rule-engine/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout);
    }
}
