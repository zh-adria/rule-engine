package com.insurance.ruleengine.domain.gateway;

/**
 * 审批流程服务网关接口
 */
public interface ApprovalFlowGateway {

    /**
     * 提交审批到 approval-flow-service
     */
    void submitApproval(String targetType, String targetId, String submittedBy, String reason);
}
