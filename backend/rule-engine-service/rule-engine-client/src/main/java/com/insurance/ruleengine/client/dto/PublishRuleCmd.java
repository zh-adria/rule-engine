package com.insurance.ruleengine.client.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PublishRuleCmd {
    @NotNull
    private Integer version;
    @NotBlank
    private String approvedBy;
    @Min(0)
    @Max(100)
    private int grayPercent;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public int getGrayPercent() { return grayPercent; }
    public void setGrayPercent(int grayPercent) { this.grayPercent = grayPercent; }
}

