package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.api.TemplateFacade;
import com.insurance.ruleengine.client.dto.RuleTemplateDTO;
import com.insurance.ruleengine.domain.gateway.TemplateGateway;
import com.insurance.ruleengine.domain.model.RuleTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateFacadeImpl — hand-rolled stub (no Mockito).
 */
class TemplateFacadeImplTest {

    private TemplateFacade facade;
    private StubGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new StubGateway();
        facade = new TemplateFacadeImpl(gateway);
    }

    @Test
    void findAll_returnsAll() {
        gateway.save(make("BMI", "BMI核保", "UNDERWRITING"));
        gateway.save(make("AUTO", "车险风控", "RISK_CONTROL"));
        assertEquals(2, facade.findAll().size());
    }

    @Test
    void findByCode_matches() {
        gateway.save(make("BMI", "BMI核保", "UNDERWRITING"));
        assertTrue(facade.findByCode("BMI").isPresent());
        assertEquals("BMI核保", facade.findByCode("BMI").get().getTemplateName());
    }

    @Test
    void findByCode_missing_returnsEmpty() {
        assertTrue(facade.findByCode("X").isEmpty());
    }

    @Test
    void findByCategory_filters() {
        gateway.save(make("BMI", "BMI核保", "UNDERWRITING"));
        assertEquals(1, facade.findByCategory("UNDERWRITING").size());
    }

    @Test
    void create_persists() {
        RuleTemplateDTO dto = new RuleTemplateDTO();
        dto.setTemplateCode("NEW"); dto.setTemplateName("新模板");
        dto.setCategory("PRICING"); dto.setBusinessLine("LIFE");
        dto.setDescription("d"); dto.setDrlTemplate("d");
        dto.setVisualTemplate("{}"); dto.setSensitive(false); dto.setOwner("a");
        RuleTemplateDTO result = facade.create(dto);
        assertNotNull(result); assertEquals("新模板", result.getTemplateName());
        assertEquals(1, gateway.size());
    }

    @Test
    void update_changesName() {
        gateway.save(make("BMI", "old", "UNDERWRITING"));
        RuleTemplateDTO patch = new RuleTemplateDTO();
        patch.setTemplateName("new");
        assertEquals("new", facade.update("BMI", patch).getTemplateName());
    }

    @Test
    void update_missing_throws() {
        RuleTemplateDTO patch = new RuleTemplateDTO();
        patch.setTemplateName("x");
        assertThrows(IllegalArgumentException.class, () -> facade.update("X", patch));
    }

    @Test
    void delete_removes() {
        gateway.save(make("BMI", "BMI核保", "UNDERWRITING"));
        assertEquals(1, gateway.size());
        facade.delete("BMI");
        assertEquals(0, gateway.size());
    }

    // ---- Stub ----

    private static class StubGateway implements TemplateGateway {
        private final Map<String, RuleTemplate> store = new LinkedHashMap<>();
        private long seq;

        int size() { return store.size(); }

        void save(RuleTemplate t) {
            if (t.getId() == null) t.setId(++seq);
            store.put(t.getTemplateCode(), t);
        }

        @Override public java.util.List<RuleTemplate> findAll() { return new java.util.ArrayList<>(store.values()); }
        @Override public Optional<RuleTemplate> findByCode(String c) { return Optional.ofNullable(store.get(c)); }
        @Override public java.util.List<RuleTemplate> findByCategory(String cat) {
            return store.values().stream().filter(t -> t.getCategory().equals(cat)).toList();
        }
        @Override public RuleTemplate create(RuleTemplate t) { save(t); return t; }
        @Override public RuleTemplate update(RuleTemplate t) { save(t); return t; }
        @Override public void delete(String c) { store.remove(c); }
    }

    private static RuleTemplate make(String code, String name, String cat) {
        RuleTemplate t = new RuleTemplate();
        t.setTemplateCode(code); t.setTemplateName(name);
        t.setCategory(cat); t.setBusinessLine("LIFE");
        t.setDescription("d"); t.setDrlTemplate("d");
        t.setVisualTemplate("{}"); t.setSensitive(false); t.setOwner("a");
        t.setId(1L); return t;
    }
}
