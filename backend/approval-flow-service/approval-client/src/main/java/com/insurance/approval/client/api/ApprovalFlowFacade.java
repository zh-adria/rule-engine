package com.insurance.approval.client.api;

import com.insurance.approval.client.dto.ApprovalDTO;
import com.insurance.approval.client.dto.ReviewApprovalCmd;
import com.insurance.approval.client.dto.SubmitApprovalCmd;

import java.util.List;

public interface ApprovalFlowFacade {

    ApprovalDTO submitApproval(SubmitApprovalCmd cmd);

    ApprovalDTO approve(Long approvalId, ReviewApprovalCmd cmd);

    ApprovalDTO reject(Long approvalId, ReviewApprovalCmd cmd);

    ApprovalDTO getApproval(Long approvalId);

    List<ApprovalDTO> listApprovals(String targetType, String targetId, String status);
}
