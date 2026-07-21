package com.insurance.ruleengine.infrastructure.drools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-1: DrlConverter bidirectional tests.
 */
class DrlConverterTest {

    private DrlConverter converter;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        converter = new DrlConverter(new ObjectMapper());
    }

    @Test
    void visualModelToDrl_emitsValidDrl() throws Exception {
        String json = "{"
                + "\"ruleName\":\"age-check\","
                + "\"packageName\":\"insurance.test\","
                + "\"salience\":90,"
                + "\"decision\":\"DECLINE\","
                + "\"conditions\":["
                + "  {\"field\":\"age\",\"operator\":\">=\",\"value\":60},"
                + "  {\"field\":\"productCode\",\"operator\":\"==\",\"value\":\"CI2026\"}"
                + "]}";

        String drl = converter.visualModelToDrl(mapper.readTree(json));

        assertTrue(drl.contains("package insurance.test"));
        assertTrue(drl.contains("rule \"age-check\""));
        assertTrue(drl.contains("salience 90"));
        assertTrue(drl.contains("this[\"age\"]"));
        assertTrue(drl.contains("this[\"productCode\"]"));
        assertTrue(drl.contains("DecisionType.DECLINE"));
    }

    @Test
    void visualModelToDrl_sanitizesRuleName() throws Exception {
        String json = "{\"ruleName\":\"my rule with spaces!\",\"conditions\":[]}";
        String drl = converter.visualModelToDrl(mapper.readTree(json));
        assertTrue(drl.contains("rule \"my_rule_with_spaces_\""));
    }

    @Test
    void drlToVisualModel_parsesSimpleRule() throws Exception {
        String drl = "package insurance.test\n"
                + "import java.util.Map;\n"
                + "import com.insurance.ruleengine.domain.model.ExecutionResult;\n"
                + "import com.insurance.ruleengine.domain.model.DecisionType;\n"
                + "rule \"age-check\"\n"
                + "salience 90\n"
                + "when\n"
                + "  $facts: Map(this[\"age\"] >= 60, this[\"productCode\"] == \"CI2026\")\n"
                + "  $result: ExecutionResult()\n"
                + "then\n"
                + "  $result.setDecision(DecisionType.DECLINE);\n"
                + "end";

        JsonNode model = converter.drlToVisualModel(drl);

        assertEquals("insurance.test", model.get("packageName").asText());
        assertEquals("age-check", model.get("ruleName").asText());
        assertEquals(90, model.get("salience").asInt());
        assertNotNull(model.get("conditions"));
        assertTrue(model.get("conditions").isArray());
        assertTrue(model.get("conditions").size() >= 1);
    }

    @Test
    void roundTrip_preservesConditions() throws Exception {
        String json = "{"
                + "\"ruleName\":\"roundtrip\","
                + "\"packageName\":\"insurance.rt\","
                + "\"salience\":50,"
                + "\"decision\":\"ACCEPT\","
                + "\"conditions\":["
                + "  {\"field\":\"bmi\",\"operator\":\">\",\"value\":30}"
                + "]}";

        String drl = converter.visualModelToDrl(mapper.readTree(json));
        JsonNode back = converter.drlToVisualModel(drl);

        assertEquals("roundtrip", back.get("ruleName").asText());
        assertEquals(50, back.get("salience").asInt());
        assertTrue(back.get("conditions").size() >= 1);
    }
}
