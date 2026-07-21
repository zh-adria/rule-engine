package com.insurance.ruleengine.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ConvertRuleRequest;
import com.insurance.ruleengine.client.dto.ConvertRuleResponse;
import com.insurance.ruleengine.infrastructure.drools.DrlConverter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Phase 4: RuleController /convert endpoint — unit level (no Spring context).
 * Verifies the controller delegates to the converter and maps response.
 */
class RuleControllerConvertTest {

    @SuppressWarnings("unchecked")
    @Test
    void controllerConvertsVisualModelToDrl() throws Exception {
        RuleEngineFacade facade = mock(RuleEngineFacade.class);
        DrlConverter converter = new DrlConverter(new ObjectMapper());

        RuleController controller = new RuleController(facade, converter, new ObjectMapper());

        ConvertRuleRequest req = new ConvertRuleRequest();
        req.setVisualModel(Map.of(
                "ruleName", "age-check",
                "packageName", "insurance.test",
                "salience", 50,
                "decision", "DECLINE",
                "outputKey", "conclusion",
                "outputValue", "too old",
                "logic", "AND",
                "conditions", List.of(Map.of("field", "age", "operator", ">=", "value", 60))
        ));

        ConvertRuleResponse resp = controller.convert(req);
        assertNotNull(resp.getDrl());
        assertTrue(resp.getDrl().contains("rule \"age-check\""));
        assertTrue(resp.getDrl().contains("this[\"age\"]"));
        assertFalse(resp.getDrl().isEmpty());
    }
}
