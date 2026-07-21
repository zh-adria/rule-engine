package com.insurance.ruleengine.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.domain.model.RuleAuditLog;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleAuditLogEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleAuditLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P1-3: audit ledger snapshot + chain hash integrity.
 * Verifies that successive audit entries form a SHA-256 chain where each
 * entry's previousHash equals the prior entry's auditHash.
 */
class AuditHashChainTest {

    private final List<RuleAuditLogEntity> store = new ArrayList<>();
    private final AtomicLong idSeq = new AtomicLong(0);
    private AuditGatewayImpl auditGateway;

    @BeforeEach
    void setUp() {
        RuleAuditLogJpaRepository repo = Mockito.mock(RuleAuditLogJpaRepository.class);

        // save: assign id and store
        when(repo.save(any(RuleAuditLogEntity.class))).thenAnswer(inv -> {
            RuleAuditLogEntity e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(idSeq.incrementAndGet());
            }
            store.add(e);
            return e;
        });

        // findByRuleCodeOrderByIdDesc: newest id first
        when(repo.findByRuleCodeOrderByIdDesc(Mockito.anyString())).thenAnswer(inv -> {
            String rc = inv.getArgument(0);
            List<RuleAuditLogEntity> result = new ArrayList<>();
            for (RuleAuditLogEntity e : store) {
                if (e.getRuleCode().equals(rc)) {
                    result.add(e);
                }
            }
            result.sort((a, b) -> Long.compare(b.getId(), a.getId()));
            return result;
        });

        // findByRuleCodeOrderByCreatedAtDesc: same order (id order ≈ created order)
        when(repo.findByRuleCodeOrderByCreatedAtDesc(Mockito.anyString())).thenAnswer(inv -> {
            String rc = inv.getArgument(0);
            List<RuleAuditLogEntity> result = new ArrayList<>();
            for (RuleAuditLogEntity e : store) {
                if (e.getRuleCode().equals(rc)) {
                    result.add(e);
                }
            }
            result.sort((a, b) -> Long.compare(b.getId(), a.getId()));
            return result;
        });

        auditGateway = new AuditGatewayImpl(repo, null, new ObjectMapper());
    }

    @Test
    void firstAuditHasGenesisPreviousHash() {
        auditGateway.recordOperation("RULE", 1, "CREATE", "alice", "init", null, null, "{\"name\":\"new\"}");

        RuleAuditLog log = auditGateway.listAudits("RULE").get(0);
        assertEquals("GENESIS", log.getPreviousHash());
        assertNotNull(log.getAuditHash());
        assertEquals(64, log.getAuditHash().length());
    }

    @Test
    void subsequentAuditsChainHashes() {
        auditGateway.recordOperation("RULE", 1, "CREATE", "a", "i", null, null, "{\"name\":\"new\"}");
        auditGateway.recordOperation("RULE", 1, "PUBLISH", "b", "p", null, null, "{\"status\":\"PUBLISHED\"}");
        auditGateway.recordOperation("RULE", 2, "ROLLBACK", "c", "r", null, null, "{\"version\":1}");

        List<RuleAuditLog> logs = auditGateway.listAudits("RULE");
        assertEquals(3, logs.size());

        RuleAuditLog newest = logs.get(0);   // ROLLBACK
        RuleAuditLog middle = logs.get(1);   // PUBLISH
        RuleAuditLog oldest = logs.get(2);   // CREATE

        assertEquals("GENESIS", oldest.getPreviousHash());
        assertEquals(oldest.getAuditHash(), middle.getPreviousHash());
        assertEquals(middle.getAuditHash(), newest.getPreviousHash());

        assertNotEquals(oldest.getAuditHash(), middle.getAuditHash());
        assertNotEquals(middle.getAuditHash(), newest.getAuditHash());
    }

    @Test
    void snapshotsAreStored() {
        auditGateway.recordOperation("RULE", 1, "PUBLISH", "b", "p", null,
                "{\"before\":\"x\"}", "{\"after\":\"y\"}");

        RuleAuditLog log = auditGateway.listAudits("RULE").get(0);
        assertEquals("{\"before\":\"x\"}", log.getBeforeJson());
        assertEquals("{\"after\":\"y\"}", log.getAfterJson());
    }

    @Test
    void differentRuleCodesAreIndependentChains() {
        auditGateway.recordOperation("RULE_A", 1, "CREATE", "a", "i", null, null, "{}");
        auditGateway.recordOperation("RULE_B", 1, "CREATE", "b", "i", null, null, "{}");

        assertEquals("GENESIS", auditGateway.listAudits("RULE_A").get(0).getPreviousHash());
        assertEquals("GENESIS", auditGateway.listAudits("RULE_B").get(0).getPreviousHash());
    }

    @Test
    void legacyRecordOperationWithoutSnapshotStillChains() {
        auditGateway.recordOperation("RULE", 1, "X", "x", "i", null);
        auditGateway.recordOperation("RULE", 2, "Y", "y", "i", null, null, "{}");

        List<RuleAuditLog> logs = auditGateway.listAudits("RULE");
        assertEquals(2, logs.size());
        assertEquals(logs.get(1).getAuditHash(), logs.get(0).getPreviousHash());
    }
}
