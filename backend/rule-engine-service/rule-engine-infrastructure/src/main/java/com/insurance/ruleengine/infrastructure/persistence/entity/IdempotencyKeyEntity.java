package com.insurance.ruleengine.infrastructure.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "re_idempotency_key", indexes = {
        @Index(name = "idx_idem_key", columnList = "idempotency_key"),
        @Index(name = "idx_idem_expires", columnList = "expires_at")
})
public class IdempotencyKeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;
    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;
    @Column(name = "resource_id", nullable = false, length = 128)
    private String resourceId;
    @Column(columnDefinition = "CLOB")
    private String responseBody;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String k) { this.idempotencyKey = k; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String t) { this.resourceType = t; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String r) { this.resourceId = r; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String b) { this.responseBody = b; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime e) { this.expiresAt = e; }
}
