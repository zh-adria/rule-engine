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
@Table(name = "re_rule_template", indexes = {
        @Index(name = "idx_template_code", columnList = "template_code"),
        @Index(name = "idx_template_category", columnList = "category"),
        @Index(name = "idx_template_business_line", columnList = "business_line")
})
public class RuleTemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "template_code", nullable = false, unique = true, length = 100)
    private String templateCode;
    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(name = "business_line", nullable = false, length = 50)
    private String businessLine;
    @Column(columnDefinition = "CLOB")
    private String description;
    @Column(columnDefinition = "CLOB")
    private String drlTemplate;
    @Column(columnDefinition = "CLOB")
    private String visualTemplate;
    @Column(nullable = false)
    private boolean sensitive;
    @Column(length = 100)
    private String owner;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @javax.persistence.PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @javax.persistence.PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public String getTemplateName() { return templateName; }
    public String getCategory() { return category; }
    public String getBusinessLine() { return businessLine; }
    public String getDescription() { return description; }
    public String getDrlTemplate() { return drlTemplate; }
    public String getVisualTemplate() { return visualTemplate; }
    public boolean isSensitive() { return sensitive; }
    public String getOwner() { return owner; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setCategory(String category) { this.category = category; }
    public void setBusinessLine(String businessLine) { this.businessLine = businessLine; }
    public void setDescription(String description) { this.description = description; }
    public void setDrlTemplate(String drlTemplate) { this.drlTemplate = drlTemplate; }
    public void setVisualTemplate(String visualTemplate) { this.visualTemplate = visualTemplate; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
