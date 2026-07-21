package com.insurance.ruleengine.domain.gateway;

import com.insurance.ruleengine.domain.model.CustomField;
import java.util.List;

public interface CustomFieldGateway {
    List<CustomField> findByBusinessLine(String businessLine);
    List<CustomField> findAll();
    CustomField save(CustomField field);
    void delete(Long id);
}
