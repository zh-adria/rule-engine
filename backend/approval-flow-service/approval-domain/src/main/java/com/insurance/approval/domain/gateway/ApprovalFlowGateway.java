package com.insurance.approval.domain.gateway;

import com.insurance.approval.domain.model.ApprovalStatus;
import com.insurance.approval.domain.model.ApprovalAction;

public interface ApprovalFlowGateway {

    ApprovalStatus transition(ApprovalStatus currentStatus, ApprovalAction action);
}
