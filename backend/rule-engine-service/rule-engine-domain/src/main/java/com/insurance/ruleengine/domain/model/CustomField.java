package com.insurance.ruleengine.domain.model;

public class CustomField {
    private Long id;
    private String fieldCode;
    private String fieldLabel;
    private String fieldType;   // string | number | boolean
    private String businessLine;
    private Integer sortOrder;
    private Boolean enabled;

    public static CustomField create(String fieldCode, String fieldLabel, String fieldType,
                                     String businessLine, Integer sortOrder) {
        CustomField f = new CustomField();
        f.fieldCode = fieldCode;
        f.fieldLabel = fieldLabel;
        f.fieldType = fieldType;
        f.businessLine = businessLine;
        f.sortOrder = sortOrder != null ? sortOrder : 0;
        f.enabled = true;
        return f;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFieldCode() { return fieldCode; }
    public void setFieldCode(String fieldCode) { this.fieldCode = fieldCode; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getBusinessLine() { return businessLine; }
    public void setBusinessLine(String businessLine) { this.businessLine = businessLine; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
