package com.insurance.ruleengine.infrastructure.persistence;

import com.insurance.ruleengine.domain.model.RuleTemplate;
import com.insurance.ruleengine.infrastructure.persistence.entity.RuleTemplateEntity;
import com.insurance.ruleengine.infrastructure.persistence.repository.RuleTemplateJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateGatewayImplTest {

    @Mock
    private RuleTemplateJpaRepository repository;

    @InjectMocks
    private TemplateGatewayImpl gateway;

    @Test
    void findAll_returnsSortedTemplates() {
        RuleTemplateEntity e1 = entity("AUTO", "车险", "RISK_CONTROL", 2);
        RuleTemplateEntity e2 = entity("BMI", "BMI", "UNDERWRITING", 1);
        when(repository.findAll()).thenReturn(Arrays.asList(e1, e2));

        List<RuleTemplate> result = gateway.findAll();
        assertEquals(2, result.size());
        assertEquals("BMI", result.get(0).getTemplateCode());
        assertEquals("AUTO", result.get(1).getTemplateCode());
    }

    @Test
    void findByCode_returnsMatching() {
        RuleTemplateEntity e = entity("BMI", "BMI核保", "UNDERWRITING", 1);
        when(repository.findByTemplateCode("BMI")).thenReturn(Optional.of(e));

        assertTrue(gateway.findByCode("BMI").isPresent());
        assertEquals("BMI核保", gateway.findByCode("BMI").get().getTemplateName());
    }

    @Test
    void findByCategory_returnsMatching() {
        RuleTemplateEntity e = entity("BMI", "BMI核保", "UNDERWRITING", 1);
        when(repository.findByCategory("UNDERWRITING")).thenReturn(Arrays.asList(e));

        assertEquals(1, gateway.findByCategory("UNDERWRITING").size());
    }

    @Test
    void create_savesAndReturns() {
        RuleTemplate template = RuleTemplate.create("NEW", "新模板", "PRICING", "LIFE", "desc", "drl", "{}", false, "admin");
        RuleTemplateEntity saved = entity("NEW", "新模板", "PRICING", 1);
        when(repository.save(any(RuleTemplateEntity.class))).thenReturn(saved);

        RuleTemplate result = gateway.create(template);
        assertNotNull(result);
        assertEquals("新模板", result.getTemplateName());
    }

    @Test
    void delete_removesTemplate() {
        RuleTemplateEntity e = entity("BMI", "BMI核保", "UNDERWRITING", 1);
        when(repository.findByTemplateCode("BMI")).thenReturn(Optional.of(e));
        doNothing().when(repository).delete(e);

        gateway.delete("BMI");
        verify(repository).delete(e);
    }

    private RuleTemplateEntity entity(String code, String name, String category, int sortOrder) {
        RuleTemplateEntity e = new RuleTemplateEntity();
        e.setTemplateCode(code);
        e.setTemplateName(name);
        e.setCategory(category);
        e.setBusinessLine("LIFE");
        e.setDescription("desc");
        e.setDrlTemplate("drl");
        e.setVisualTemplate("{}");
        e.setSensitive(false);
        e.setOwner("admin");
        e.setSortOrder(sortOrder);
        e.setId(1L);
        return e;
    }
}
