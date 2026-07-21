package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.gateway.IdempotencyGateway;
import com.insurance.ruleengine.domain.model.IdempotencyRecord;
import com.insurance.ruleengine.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class IdempotencyGatewayImpl implements IdempotencyGateway {
    private final IdempotencyKeyJpaRepository repository;

    public IdempotencyGatewayImpl(IdempotencyKeyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        IdempotencyKeyEntity entity = toEntity(record);
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteExpired() {
        repository.deleteExpired(LocalDateTime.now());
    }

    private IdempotencyRecord toDomain(IdempotencyKeyEntity entity) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.setId(entity.getId());
        r.setIdempotencyKey(entity.getIdempotencyKey());
        r.setResourceType(entity.getResourceType());
        r.setResourceId(entity.getResourceId());
        r.setResponseBody(entity.getResponseBody());
        r.setCreatedAt(entity.getCreatedAt());
        r.setExpiresAt(entity.getExpiresAt());
        return r;
    }

    private IdempotencyKeyEntity toEntity(IdempotencyRecord record) {
        IdempotencyKeyEntity e = new IdempotencyKeyEntity();
        e.setId(record.getId());
        e.setIdempotencyKey(record.getIdempotencyKey());
        e.setResourceType(record.getResourceType());
        e.setResourceId(record.getResourceId());
        e.setResponseBody(record.getResponseBody());
        e.setCreatedAt(record.getCreatedAt());
        e.setExpiresAt(record.getExpiresAt());
        return e;
    }
}
