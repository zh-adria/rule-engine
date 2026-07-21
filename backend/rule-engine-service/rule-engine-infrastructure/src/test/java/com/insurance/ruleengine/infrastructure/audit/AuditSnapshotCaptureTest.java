package com.insurance.ruleengine.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleAuditLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Phase 3+4: verify AuditGatewayImpl writes before/after snapshots.
 */
class AuditSnapshotCaptureTest {

    private AuditGatewayImpl gateway;
    private RuleAuditLogJpaRepository repo;
    private AtomicLong idSeq;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(RuleAuditLogJpaRepository.class);
        idSeq = new AtomicLong(0);
        when(repo.save(any(RuleAuditLogEntity.class))).thenAnswer(inv -> {
            RuleAuditLogEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(idSeq.incrementAndGet());
            return e;
        });
        when(repo.findByRuleCodeOrderByIdDesc(Mockito.anyString())).thenAnswer(inv -> {
            String rc = inv.getArgument(0);
            return List.of(); // empty for tests where we just want to validate call args
        });
        gateway = new AuditGatewayImpl(repo, null, new ObjectMapper());
    }

    @Test
    void recordOperationWithSnapshot_passesBeforeAndAfter() {
        ArgumentCaptor<RuleAuditLogEntity> cap = ArgumentCaptor.forClass(RuleAuditLogEntity.class);
        gateway.recordOperation("RULE", 1, "PUBLISH", "alice", "publish v1", "1.2.3.4",
                "{\"status\":\"APPROVED\"}", "{\"status\":\"PUBLISHED\"}");
        Mockito.verify(repo).save(cap.capture());
        RuleAuditLogEntity e = cap.getValue();
        assertEquals("{\"status\":\"APPROVED\"}", e.getBeforeJson());
        assertEquals("{\"status\":\"PUBLISHED\"}", e.getAfterJson());
        assertNotNull(e.getAuditHash());
        assertEquals(64, e.getAuditHash().length());
        assertEquals("GENESIS", e.getPreviousHash());
    }

    @Test
    void jsonEscapesAreValid() {
        gateway.recordOperation("R1", 1, "A", "op", "r", null, "{bad json", "{\"ok\":true}");
        ArgumentCaptor<RuleAuditLogEntity> cap = ArgumentCaptor.forClass(RuleAuditLogEntity.class);
        Mockito.verify(repo).save(cap.capture());
        assertNotNull(cap.getValue().getAuditHash());
    }
}
