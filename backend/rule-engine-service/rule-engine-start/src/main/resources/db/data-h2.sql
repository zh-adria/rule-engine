MERGE INTO sys_user (
  username,
  password,
  display_name,
  role,
  enabled,
  created_at
) KEY(username) VALUES (
  'admin',
  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
  '管理员',
  'ADMIN',
  TRUE,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_definition (
  rule_code,
  rule_name,
  category,
  business_line,
  description,
  sensitive,
  owner,
  current_version,
  gray_version,
  gray_percent,
  regulatory_ref,
  created_at,
  updated_at
) KEY(rule_code) VALUES (
  'CI_UW_HEALTH_2026',
  '重疾险健康告知核保规则',
  'UNDERWRITING',
  'CRITICAL_ILLNESS',
  '重疾险健康告知、BMI、职业类别核保',
  FALSE,
  'underwriting-team',
  1,
  NULL,
  0,
  'CBIRC-INSURANCE-SALES-TRACE',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_definition (
  rule_code,
  rule_name,
  category,
  business_line,
  description,
  sensitive,
  owner,
  current_version,
  gray_version,
  gray_percent,
  regulatory_ref,
  created_at,
  updated_at
) KEY(rule_code) VALUES (
  'FRAUD_BLACKLIST_2026',
  '反欺诈黑名单与异常行为风控规则',
  'RISK_CONTROL',
  'ONLINE_APPLICATION',
  '黑名单命中、同设备多次投保、近12个月理赔频次异常风控',
  FALSE,
  'risk-control-team',
  1,
  NULL,
  0,
  'CBIRC-AML-RISK-CONTROL',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_definition (
  rule_code,
  rule_name,
  category,
  business_line,
  description,
  sensitive,
  owner,
  current_version,
  gray_version,
  gray_percent,
  regulatory_ref,
  created_at,
  updated_at
) KEY(rule_code) VALUES (
  'CI_PRODUCT_PRICING_2026',
  '重疾险产品定价与代理人佣金规则',
  'PRODUCT_PRICING',
  'CRITICAL_ILLNESS',
  '重疾险基础保费、吸烟人群费率上浮、代理人佣金比例计算',
  FALSE,
  'product-pricing-team',
  1,
  NULL,
  0,
  'CBIRC-PRODUCT-PRICING-FILING',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_version (
  rule_code,
  version,
  status,
  drl_content,
  visual_model,
  checksum,
  effective_from,
  effective_to,
  created_by,
  approved_by,
  created_at,
  published_at
) KEY(rule_code, version) VALUES (
  'CI_UW_HEALTH_2026',
  1,
  'PUBLISHED',
  'package insurance.underwriting

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

global java.util.List regulatoryNotes;

rule "CI_UW_001 diabetes with complications decline"
salience 100
when
  $facts: Map(this["productCode"] == "CI2026",
              this["hasDiabetes"] == true,
              this["hasComplication"] == true)
  $result: ExecutionResult()
then
  $result.setDecision(DecisionType.DECLINE);
  $result.getHitRules().add("CI_UW_001");
  $result.getOutputs().put("underwritingConclusion", "糖尿病且存在并发症，重疾险拒保");
  $result.getOutputs().put("manualReason", "健康告知高风险疾病组合");
end

rule "CI_UW_002 BMI high manual review"
salience 80
when
  $facts: Map(this["productCode"] == "CI2026",
              this["bmi"] != null,
              ((Number)this["bmi"]).doubleValue() >= 32.0)
  $result: ExecutionResult(decision != DecisionType.DECLINE)
then
  $result.setDecision(DecisionType.MANUAL_REVIEW);
  $result.getHitRules().add("CI_UW_002");
  $result.getOutputs().put("underwritingConclusion", "BMI超过核保阈值，进入人工核保");
end

rule "CI_UW_003 hazardous occupation rate up"
salience 70
when
  $facts: Map(this["occupationClass"] != null,
              ((Number)this["occupationClass"]).intValue() >= 5)
  $result: ExecutionResult(decision == DecisionType.ACCEPT)
then
  $result.setDecision(DecisionType.RATE_UP);
  $result.getHitRules().add("CI_UW_003");
  $result.getOutputs().put("extraPremiumRate", 0.25);
  $result.getOutputs().put("underwritingConclusion", "5-6类职业加费承保");
end

rule "CI_UW_004 clean disclosure accept"
salience 10
when
  $facts: Map(this["productCode"] == "CI2026")
  $result: ExecutionResult(hitRules.size == 0)
then
  $result.setDecision(DecisionType.ACCEPT);
  $result.getHitRules().add("CI_UW_004");
  $result.getOutputs().put("underwritingConclusion", "健康告知无异常，标准体承保");
end
',
  '{"nodes":[{"field":"productCode","operator":"==","value":"CI2026"},{"field":"hasDiabetes","operator":"==","value":"true"},{"field":"hasComplication","operator":"==","value":"true"},{"field":"bmi","operator":">=","value":"32"},{"field":"occupationClass","operator":">=","value":"5"}],"outputs":["underwritingConclusion","manualReason","extraPremiumRate"]}',
  'seed-h2-ci-uw-v1',
  NULL,
  NULL,
  'system',
  'system',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_version (
  rule_code,
  version,
  status,
  drl_content,
  visual_model,
  checksum,
  effective_from,
  effective_to,
  created_by,
  approved_by,
  created_at,
  published_at
) KEY(rule_code, version) VALUES (
  'FRAUD_BLACKLIST_2026',
  1,
  'PUBLISHED',
  'package insurance.risk

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

rule "FRAUD_001 blacklist hit"
salience 100
when
  $facts: Map(this["blacklisted"] == true)
  $result: ExecutionResult()
then
  $result.setDecision(DecisionType.BLACKLIST_HIT);
  $result.getHitRules().add("FRAUD_001");
  $result.getOutputs().put("riskLevel", "REJECT");
  $result.getOutputs().put("riskReason", "投保人命中公司黑名单");
end

rule "FRAUD_002 device multi policy manual review"
salience 80
when
  $facts: Map(this["devicePolicyCount24h"] != null,
              ((Number)this["devicePolicyCount24h"]).intValue() >= 5)
  $result: ExecutionResult(decision != DecisionType.BLACKLIST_HIT)
then
  $result.setDecision(DecisionType.MANUAL_REVIEW);
  $result.getHitRules().add("FRAUD_002");
  $result.getOutputs().put("riskLevel", "HIGH");
  $result.getOutputs().put("riskReason", "同设备24小时内多次投保");
end

rule "FRAUD_003 abnormal claim history"
salience 60
when
  $facts: Map(this["claimCount12m"] != null,
              ((Number)this["claimCount12m"]).intValue() >= 3)
  $result: ExecutionResult(decision == DecisionType.ACCEPT)
then
  $result.setDecision(DecisionType.MANUAL_REVIEW);
  $result.getHitRules().add("FRAUD_003");
  $result.getOutputs().put("riskLevel", "MEDIUM");
  $result.getOutputs().put("riskReason", "近12个月理赔频次异常");
end
',
  '{"nodes":[{"field":"blacklisted","operator":"==","value":"true"},{"field":"devicePolicyCount24h","operator":">=","value":"5"},{"field":"claimCount12m","operator":">=","value":"3"}],"outputs":["riskLevel","riskReason"]}',
  'seed-h2-fraud-v1',
  NULL,
  NULL,
  'system',
  'system',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_version (
  rule_code,
  version,
  status,
  drl_content,
  visual_model,
  checksum,
  effective_from,
  effective_to,
  created_by,
  approved_by,
  created_at,
  published_at
) KEY(rule_code, version) VALUES (
  'CI_PRODUCT_PRICING_2026',
  1,
  'PUBLISHED',
  'package insurance.pricing

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

rule "PRICE_001 critical illness base premium"
salience 100
when
  $facts: Map(this["productCode"] == "CI2026",
              this["age"] != null,
              this["sumInsured"] != null)
  $result: ExecutionResult()
then
  int age = ((Number)$facts.get("age")).intValue();
  double sumInsured = ((Number)$facts.get("sumInsured")).doubleValue();
  double baseRate = age < 30 ? 0.0032 : (age < 45 ? 0.0068 : 0.0125);
  $result.setDecision(DecisionType.PRICE_ADJUST);
  $result.getHitRules().add("PRICE_001");
  $result.getOutputs().put("basePremium", sumInsured * baseRate);
end

rule "PRICE_002 smoker surcharge"
salience 80
when
  $facts: Map(this["smoker"] == true)
  $result: ExecutionResult(outputs["basePremium"] != null)
then
  double basePremium = ((Number)$result.getOutputs().get("basePremium")).doubleValue();
  $result.getHitRules().add("PRICE_002");
  $result.getOutputs().put("finalPremium", basePremium * 1.2);
  $result.getOutputs().put("pricingReason", "吸烟人群费率上浮20%");
end

rule "PRICE_003 agent commission"
salience 50
when
  $facts: Map(this["channel"] == "AGENT")
  $result: ExecutionResult(outputs["basePremium"] != null)
then
  double premium = $result.getOutputs().get("finalPremium") == null
      ? ((Number)$result.getOutputs().get("basePremium")).doubleValue()
      : ((Number)$result.getOutputs().get("finalPremium")).doubleValue();
  $result.getHitRules().add("PRICE_003");
  $result.getOutputs().put("commission", premium * 0.18);
end
',
  '{"nodes":[{"field":"productCode","operator":"==","value":"CI2026"},{"field":"age","operator":"exists","value":"true"},{"field":"sumInsured","operator":"exists","value":"true"},{"field":"smoker","operator":"==","value":"true"},{"field":"channel","operator":"==","value":"AGENT"}],"outputs":["basePremium","finalPremium","pricingReason","commission"]}',
  'seed-h2-pricing-v1',
  NULL,
  NULL,
  'system',
  'system',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

MERGE INTO rule_audit_log KEY(id) VALUES (
  9001,
  'CI_UW_HEALTH_2026',
  1,
  'SEED_PUBLISH',
  'system',
  'H2 local seed data: underwriting rules',
  '127.0.0.1',
  CURRENT_TIMESTAMP
);

MERGE INTO rule_audit_log KEY(id) VALUES (
  9002,
  'FRAUD_BLACKLIST_2026',
  1,
  'SEED_PUBLISH',
  'system',
  'H2 local seed data: risk control rules',
  '127.0.0.1',
  CURRENT_TIMESTAMP
);

MERGE INTO rule_audit_log KEY(id) VALUES (
  9003,
  'CI_PRODUCT_PRICING_2026',
  1,
  'SEED_PUBLISH',
  'system',
  'H2 local seed data: product pricing and commission rules',
  '127.0.0.1',
  CURRENT_TIMESTAMP
);
