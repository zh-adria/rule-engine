package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.IdempotencyRecord;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyGateway {
    Optional<IdempotencyRecord> findByKey(String idempotencyKey);
    IdempotencyRecord save(IdempotencyRecord record);
    void deleteExpired();
}
