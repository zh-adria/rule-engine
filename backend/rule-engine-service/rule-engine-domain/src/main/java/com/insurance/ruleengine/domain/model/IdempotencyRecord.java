package com.insurance.ruleengine.domain.model;

public class IdempotencyRecord {
    private Long id;
    private String idempotencyKey;
    private String resourceType;
    private String resourceId;
    private String responseBody;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime expiresAt;

    public static IdempotencyRecord create(String key, String resourceType, String resourceId,
                                           String responseBody, java.time.Duration ttl) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.idempotencyKey = key;
        r.resourceType = resourceType;
        r.resourceId = resourceId;
        r.responseBody = responseBody;
        r.createdAt = java.time.LocalDateTime.now();
        r.expiresAt = r.createdAt.plus(ttl);
        return r;
    }

    public boolean isExpired() { return java.time.LocalDateTime.now().isAfter(expiresAt); }
    // getters/setters
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
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime c) { this.createdAt = c; }
    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.LocalDateTime e) { this.expiresAt = e; }
}
