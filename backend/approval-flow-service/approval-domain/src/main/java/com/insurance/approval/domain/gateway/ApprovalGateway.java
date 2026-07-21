package com.insurance.approval.domain.gateway;

import com.insurance.approval.domain.model.ApprovalRecord;

import java.util.List;
import java.util.Optional;

public interface ApprovalGateway {

    ApprovalRecord save(ApprovalRecord record);

    Optional<ApprovalRecord> findById(Long id);

    List<ApprovalRecord> findByTarget(String targetType, String targetId);

    List<ApprovalRecord> findByStatus(String status);
}
