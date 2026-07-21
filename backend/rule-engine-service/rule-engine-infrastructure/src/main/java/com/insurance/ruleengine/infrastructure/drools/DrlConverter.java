package com.insurance.ruleengine.infrastructure.drools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.drools.drl.ast.descr.AndDescr;
import org.drools.drl.ast.descr.AttributeDescr;
import org.drools.drl.ast.descr.BaseDescr;
import org.drools.drl.ast.descr.ConditionalElementDescr;
import org.drools.drl.ast.descr.ExprConstraintDescr;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.drl.ast.descr.PatternDescr;
import org.drools.drl.ast.descr.RuleDescr;
import org.drools.drl.parser.DrlParser;
import org.drools.drl.parser.DroolsParserException;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * P1-1: bidirectional converter between visual-rule JSON model and DRL text.
 *
 * Visual model shape:
 * <pre>
 * {
 *   "ruleName": "...",
 *   "packageName": "insurance.x",
 *   "salience": 80,
 *   "decision": "MANUAL_REVIEW",
 *   "logic": "AND",
 *   "outputKey": "conclusion",
 *   "outputValue": "规则命中",
 *   "conditions": [
 *     { "field": "age", "operator": ">=", "value": 60 },
 *     { "field": "productCode", "operator": "==", "value": "CI2026" }
 *   ]
 * }
 * </pre>
 */
@Component
public class DrlConverter {

    private static final String FACT_VAR = "$facts";
    private static final String RESULT_VAR = "$result";

    private final ObjectMapper objectMapper;

    public DrlConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert a visual-rule JSON model to DRL text.
     *
     * @param visualModel visual-rule JSON (see class javadoc)
     * @return DRL string ready for Drools compiler
     */
    public String visualModelToDrl(JsonNode visualModel) {
        String safeRuleName = safeString(visualModel, "ruleName", "Rule").replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String pkg = safeString(visualModel, "packageName", "insurance.rules");
        int salience = visualModel.has("salience") ? visualModel.get("salience").asInt(80) : 80;
        String decision = safeString(visualModel, "decision", "MANUAL_REVIEW");
        String outputKey = safeString(visualModel, "outputKey", "conclusion");
        String outputValue = safeString(visualModel, "outputValue", "规则命中");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append("\n\n");
        sb.append("import java.util.Map;\n");
        sb.append("import com.insurance.ruleengine.domain.model.ExecutionResult;\n");
        sb.append("import com.insurance.ruleengine.domain.model.DecisionType;\n\n");
        sb.append("rule \"").append(safeRuleName).append("\"\n");
        sb.append("salience ").append(salience).append("\n");
        sb.append("when\n");
        sb.append("  ").append(FACT_VAR).append(": Map(\n");

        ArrayNode conditions = visualModel.has("conditions") && visualModel.get("conditions").isArray()
                ? (ArrayNode) visualModel.get("conditions")
                : objectMapper.createArrayNode();

        List<String> clauses = new ArrayList<>();
        for (JsonNode cond : conditions) {
            String clause = toWhenClause(cond);
            if (clause != null && !clause.isBlank()) {
                clauses.add(clause);
            }
        }
        sb.append(String.join("," + System.lineSeparator(), clauses));
        sb.append(")\n");
        sb.append("  ").append(RESULT_VAR).append(": ExecutionResult(decision != DecisionType.DECLINE)\n");
        sb.append("then\n");
        sb.append("  ").append(RESULT_VAR).append(".setDecision(DecisionType.").append(decision).append(");\n");
        sb.append("  ").append(RESULT_VAR).append(".getHitRules().add(\"").append(safeRuleName).append("\");\n");
        sb.append("  ").append(RESULT_VAR).append(".getOutputs().put(\"").append(outputKey).append("\", \"").append(outputValue).append("\");\n");
        sb.append("end\n");
        return sb.toString();
    }

    /**
     * Parse a DRL string back into a visual-rule JSON model (best-effort: extracts first rule only).
     *
     * @param drl DRL source
     * @return visual-rule JSON parsed from the DRL
     * @throws IllegalArgumentException if DRL is unparsable
     */
    public JsonNode drlToVisualModel(String drl) {
        Resource resource = ResourceFactory.newByteArrayResource(drl.getBytes());
        DrlParser parser = new DrlParser();
        try {
            PackageDescr pkg = parser.parse(resource);
            if (parser.hasErrors() || pkg.getRules().isEmpty()) {
                throw new IllegalArgumentException("DRL parse failed: " + parser.getErrors());
            }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("packageName", pkg.getName() == null ? "insurance.rules" : pkg.getName());

        RuleDescr rule = pkg.getRules().get(0);
        result.put("ruleName", rule.getName());

        String salience = rule.getSalience();
        if (salience != null && !salience.isBlank()) {
            try {
                result.put("salience", Integer.parseInt(salience.trim()));
            } catch (NumberFormatException ignored) {
                // mvel expression salience (rare for visual rules) — skip
            }
        }

        List<String> fieldNames = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        List<String> values = new ArrayList<>();

        PatternDescr factPattern = findFactPattern(rule);
        if (factPattern != null) {
            collectConstraints(factPattern.getConstraint(), fieldNames, operators, values);
        }

        ArrayNode conditions = objectMapper.createArrayNode();
        int n = Math.min(fieldNames.size(), Math.min(operators.size(), values.size()));
        for (int i = 0; i < n; i++) {
            ObjectNode cond = objectMapper.createObjectNode();
            cond.put("field", fieldNames.get(i));
            cond.put("operator", operators.get(i));
            cond.put("value", values.get(i));
            conditions.add(cond);
        }
            result.set("conditions", conditions);

            return result;
        } catch (DroolsParserException | java.io.IOException e) {
            throw new IllegalArgumentException("DRL parse error: " + e.getMessage(), e);
        }
    }

    // ----- private helpers -----

    private String toWhenClause(JsonNode cond) {
        String field = safeString(cond, "field", null);
        String op = safeString(cond, "operator", null);
        JsonNode valueNode = cond.get("value");
        if (field == null || op == null || valueNode == null || valueNode.isNull()) {
            return null;
        }

        String accessor = "this[\"" + field + "\"]";
        switch (op) {
            case "==":
                return "              " + accessor + " == " + toLiteral(valueNode);
            case "!=":
                return "              " + accessor + " != " + toLiteral(valueNode);
            case ">":
            case ">=":
            case "<":
            case "<=":
                return "              " + accessor + " != null,\n"
                        + "              ((Number)" + accessor + ").doubleValue() " + op + " " + valueNode.asDouble();
            case "in":
            case "not in":
                StringBuilder sb = new StringBuilder();
                if ("not in".equals(op)) {
                    sb.append("!(");
                }
                sb.append(accessor).append(" in (");
                List<String> items = new ArrayList<>();
                for (JsonNode v : valueNode) {
                    items.add(toLiteral(v));
                }
                sb.append(String.join(", ", items));
                sb.append(")");
                if ("not in".equals(op)) {
                    sb.append(")");
                }
                return "              " + sb;
            default:
                return null;
        }
    }

    private String toLiteral(JsonNode node) {
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        if (node.isBoolean()) {
            return Boolean.toString(node.booleanValue());
        }
        String text = node.asText().replace("\"", "\\\"");
        return "\"" + text + "\"";
    }

    private String safeString(JsonNode node, String field, String fallback) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : fallback;
    }

    private PatternDescr findFactPattern(RuleDescr rule) {
        AndDescr lhs = rule.getLhs();
        if (lhs == null) {
            return null;
        }
        for (BaseDescr child : lhs.getDescrs()) {
            if (child instanceof PatternDescr) {
                PatternDescr p = (PatternDescr) child;
                if (p.getConstraint() != null && !p.getConstraint().getDescrs().isEmpty()) {
                    return p;
                }
            }
        }
        return null;
    }

    private void collectConstraints(ConditionalElementDescr group, List<String> fields, List<String> ops, List<String> values) {
        for (BaseDescr child : group.getDescrs()) {
            if (child instanceof ConditionalElementDescr) {
                collectConstraints((ConditionalElementDescr) child, fields, ops, values);
            } else if (child instanceof ExprConstraintDescr) {
                String text = ((ExprConstraintDescr) child).getText();
                if (text != null) {
                    parseExpr(text, fields, ops, values);
                }
            }
        }
    }

    private void parseExpr(String text, List<String> fields, List<String> ops, List<String> values) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("this\\[\"([^\"]+)\"\\]\\s*(==|!=|>=|<=|>|<)\\s*(.+)")
                .matcher(text);
        if (m.find()) {
            fields.add(m.group(1));
            ops.add(m.group(2));
            String v = m.group(3).trim();
            if (v.endsWith(",")) {
                v = v.substring(0, v.length() - 1).trim();
            }
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            values.add(v);
        }
    }
}
