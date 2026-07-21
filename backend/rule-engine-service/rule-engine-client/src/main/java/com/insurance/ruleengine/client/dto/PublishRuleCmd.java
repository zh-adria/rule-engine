package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class PublishRuleCmd {
    @NotNull
    private Integer version;
    @NotBlank
    private String approvedBy;
    @Min(0)
    @Max(100)
    private int grayPercent;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String idempotencyKey;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public int getGrayPercent() { return grayPercent; }
    public void setGrayPercent(int grayPercent) { this.grayPercent = grayPercent; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
