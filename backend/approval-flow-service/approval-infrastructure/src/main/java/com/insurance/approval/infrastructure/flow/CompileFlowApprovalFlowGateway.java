package com.insurance.approval.infrastructure.flow;

import com.alibaba.compileflow.engine.ProcessEngine;
import com.alibaba.compileflow.engine.ProcessEngineFactory;
import com.insurance.approval.domain.gateway.ApprovalFlowGateway;
import com.insurance.approval.domain.model.ApprovalAction;
import com.insurance.approval.domain.model.ApprovalStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CompileFlowApprovalFlowGateway implements ApprovalFlowGateway {
    static final String FLOW_CODE = "approval.approvalFlow";

    private final ProcessEngine<?> processEngine;

    public CompileFlowApprovalFlowGateway() {
        this(ProcessEngineFactory.getProcessEngine());
    }

    CompileFlowApprovalFlowGateway(ProcessEngine<?> processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public ApprovalStatus transition(ApprovalStatus currentStatus, ApprovalAction action) {
        Map<String, Object> context = new HashMap<>();
        context.put("currentStatus", currentStatus.name());
        context.put("action", action.name());
        Map<String, Object> result = processEngine.execute(FLOW_CODE, context);
        Object nextStatus = result.get("approvalStatus");
        if (!(nextStatus instanceof String)) {
            throw new IllegalStateException("approval flow did not return approvalStatus");
        }
        return ApprovalStatus.valueOf((String) nextStatus);
    }
}
