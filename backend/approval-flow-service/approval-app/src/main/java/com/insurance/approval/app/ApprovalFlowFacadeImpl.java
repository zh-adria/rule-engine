package com.insurance.approval.app;

import com.insurance.approval.client.api.ApprovalFlowFacade;
import com.insurance.approval.client.dto.ApprovalDTO;
import com.insurance.approval.client.dto.ReviewApprovalCmd;
import com.insurance.approval.client.dto.SubmitApprovalCmd;
import com.insurance.approval.domain.gateway.ApprovalFlowGateway;
import com.insurance.approval.domain.gateway.ApprovalGateway;
import com.insurance.approval.domain.gateway.CallbackGateway;
import com.insurance.approval.domain.model.ApprovalAction;
import com.insurance.approval.domain.model.ApprovalRecord;
import com.insurance.approval.domain.service.ApprovalPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalFlowFacadeImpl implements ApprovalFlowFacade {

    private final ApprovalGateway approvalGateway;
    private final ApprovalFlowGateway approvalFlowGateway;
    private final CallbackGateway callbackGateway;
    private final ApprovalPolicy approvalPolicy = new ApprovalPolicy();

    public ApprovalFlowFacadeImpl(ApprovalGateway approvalGateway,
                                  ApprovalFlowGateway approvalFlowGateway,
                                  CallbackGateway callbackGateway) {
        this.approvalGateway = approvalGateway;
        this.approvalFlowGateway = approvalFlowGateway;
        this.callbackGateway = callbackGateway;
    }

    @Override
    @Transactional
    public ApprovalDTO submitApproval(SubmitApprovalCmd cmd) {
        approvalPolicy.validateTargetType(cmd.getTargetType());
        approvalPolicy.validateTargetId(cmd.getTargetId());
        approvalPolicy.validateSubmittedBy(cmd.getSubmittedBy());

        ApprovalRecord record = ApprovalRecord.create(
                cmd.getTargetType(), cmd.getTargetId(), cmd.getSubmittedBy(), cmd.getReason());
        ApprovalRecord saved = approvalGateway.save(record);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ApprovalDTO approve(Long approvalId, ReviewApprovalCmd cmd) {
        approvalPolicy.validateReviewedBy(cmd.getReviewedBy());

        ApprovalRecord record = approvalGateway.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("审批记录不存在: " + approvalId));

        record.approve(cmd.getReviewedBy(), cmd.getReason());
        ApprovalRecord saved = approvalGateway.save(record);

        // Callback to rule-engine-service
        callbackGateway.notifyApprovalResult(saved.getTargetType(), saved.getTargetId(),
                "APPROVED", cmd.getReviewedBy(), cmd.getReason());

        return toDTO(saved);
    }

    @Override
    @Transactional
    public ApprovalDTO reject(Long approvalId, ReviewApprovalCmd cmd) {
        approvalPolicy.validateReviewedBy(cmd.getReviewedBy());

        ApprovalRecord record = approvalGateway.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("审批记录不存在: " + approvalId));

        record.reject(cmd.getReviewedBy(), cmd.getReason());
        ApprovalRecord saved = approvalGateway.save(record);

        // Callback to rule-engine-service
        callbackGateway.notifyApprovalResult(saved.getTargetType(), saved.getTargetId(),
                "REJECTED", cmd.getReviewedBy(), cmd.getReason());

        return toDTO(saved);
    }

    @Override
    public ApprovalDTO getApproval(Long approvalId) {
        ApprovalRecord record = approvalGateway.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("审批记录不存在: " + approvalId));
        return toDTO(record);
    }

    @Override
    public List<ApprovalDTO> listApprovals(String targetType, String targetId, String status) {
        List<ApprovalRecord> records;
        if (status != null && !status.isBlank()) {
            records = approvalGateway.findByStatus(status);
        } else if (targetType != null && targetId != null) {
            records = approvalGateway.findByTarget(targetType, targetId);
        } else {
            records = approvalGateway.findByStatus(null);
        }
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ApprovalDTO toDTO(ApprovalRecord record) {
        ApprovalDTO dto = new ApprovalDTO();
        dto.setId(record.getId());
        dto.setTargetType(record.getTargetType());
        dto.setTargetId(record.getTargetId());
        dto.setStatus(record.getStatus().name());
        dto.setSubmittedBy(record.getSubmittedBy());
        dto.setReviewedBy(record.getReviewedBy());
        dto.setReason(record.getReason());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }
}
