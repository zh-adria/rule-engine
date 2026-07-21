package com.insurance.approval.infrastructure.persistence;

import com.insurance.approval.domain.gateway.ApprovalGateway;
import com.insurance.approval.domain.model.ApprovalRecord;
import com.insurance.approval.domain.model.ApprovalStatus;
import com.insurance.approval.infrastructure.persistence.entity.ApprovalRecordEntity;
import com.insurance.approval.infrastructure.persistence.repository.ApprovalRecordJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ApprovalGatewayImpl implements ApprovalGateway {

    private final ApprovalRecordJpaRepository repository;

    public ApprovalGatewayImpl(ApprovalRecordJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ApprovalRecord save(ApprovalRecord record) {
        ApprovalRecordEntity entity = toEntity(record);
        ApprovalRecordEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ApprovalRecord> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApprovalRecord> findByTarget(String targetType, String targetId) {
        return repository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRecord> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            return repository.findAll().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        }
        return repository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private ApprovalRecord toDomain(ApprovalRecordEntity entity) {
        ApprovalRecord record = ApprovalRecord.create(
                entity.getTargetType(), entity.getTargetId(),
                entity.getSubmittedBy(), entity.getReason());
        record.setId(entity.getId());
        record.setStatus(ApprovalStatus.valueOf(entity.getStatus()));
        // Restore multi-level approval state
        record.setCurrentLevel(entity.getCurrentLevel() != null ? entity.getCurrentLevel() : 1);
        record.setMaxLevel(entity.getMaxLevel() != null ? entity.getMaxLevel() : 1);
        record.setLevelApprovedBy(entity.getLevelApprovedBy());
        record.setLevelApprovedAt(entity.getLevelApprovedAt());
        record.setApprovalChain(entity.getApprovalChain());
        // Use approve/reject to set reviewedBy if present
        if (entity.getReviewedBy() != null) {
            if ("APPROVED".equals(entity.getStatus())) {
                record.approve(entity.getReviewedBy(), entity.getReason());
            } else if ("REJECTED".equals(entity.getStatus())) {
                record.reject(entity.getReviewedBy(), entity.getReason());
            }
        }
        return record;
    }

    private ApprovalRecordEntity toEntity(ApprovalRecord record) {
        ApprovalRecordEntity entity = new ApprovalRecordEntity();
        entity.setId(record.getId());
        entity.setTargetType(record.getTargetType());
        entity.setTargetId(record.getTargetId());
        entity.setStatus(record.getStatus().name());
        entity.setSubmittedBy(record.getSubmittedBy());
        entity.setReviewedBy(record.getReviewedBy());
        entity.setReason(record.getReason());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setUpdatedAt(record.getUpdatedAt());
        entity.setCurrentLevel(record.getCurrentLevel());
        entity.setMaxLevel(record.getMaxLevel());
        entity.setLevelApprovedBy(record.getLevelApprovedBy());
        entity.setLevelApprovedAt(record.getLevelApprovedAt());
        entity.setApprovalChain(record.getApprovalChain());
        return entity;
    }
}
