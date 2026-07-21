package com.insurance.approval.infrastructure.persistence.repository;

import com.insurance.approval.infrastructure.persistence.entity.ApprovalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRecordJpaRepository extends JpaRepository<ApprovalRecordEntity, Long> {

    List<ApprovalRecordEntity> findByTargetTypeAndTargetId(String targetType, String targetId);

    List<ApprovalRecordEntity> findByStatus(String status);
}
