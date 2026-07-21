package com.insurance.ruleengine.adapter.grpc;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import org.springframework.stereotype.Component;

@Component
public class GrpcRuleEngineAdapter {
    private final RuleEngineFacade ruleEngineFacade;

    public GrpcRuleEngineAdapter(RuleEngineFacade ruleEngineFacade) {
        this.ruleEngineFacade = ruleEngineFacade;
    }

    public RuleEngineFacade facade() {
        return ruleEngineFacade;
    }
}

