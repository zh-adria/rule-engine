-- Rule Template Library (P1 1.2)
CREATE TABLE IF NOT EXISTS re_rule_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code   VARCHAR(100) NOT NULL UNIQUE,
    template_name   VARCHAR(200) NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    business_line   VARCHAR(50)  NOT NULL,
    description     TEXT,
    drl_template    TEXT,
    visual_template TEXT,
    sensitive       BOOLEAN DEFAULT FALSE,
    owner           VARCHAR(100),
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_business_line (business_line)
);

INSERT INTO re_rule_template (template_code, template_name, category, business_line, description, drl_template, visual_template, sensitive, owner, sort_order) VALUES
('BMI_UNDERWRITING', 'BMI 核保规则', 'UNDERWRITING', 'LIFE',
 '基于 BMI 指数的寿险核保规则模板',
 'package com.insurance.ruleengine\n\nimport com.insurance.ruleengine.*\n\ndialect "java"\n\nglobal com.insurance.ruleengine.domain.model.DecisionType decisionType;\n\nglobal java.util.List<String> hitRules;\n\nglobal java.util.Map<String, Object> outputs;\n\nglobal StringBuilder auditLog;\n\nrule "BMI_UNDERWRITING_HEALTHY"\nwhen\n    $f : RiskFact(bmi != null, bmi < 24)\n    $f.getAge() >= 18 && $f.getAge() <= 60\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.ACCEPT;\n    hitRules.add("BMI_UNDERWRITING_HEALTHY");\n    outputs.put("riskLevel", "LOW");\n    outputs.put("bmiCategory", "NORMAL");\n    outputs.put("message", "BMI 正常，标准体承保");\nend\n\nrule "BMI_UNDERWRITING_OVERWEIGHT"\nwhen\n    $f : RiskFact(bmi != null, bmi >= 24, bmi < 28)\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.ACCEPT;\n    hitRules.add("BMI_UNDERWRITING_OVERWEIGHT");\n    outputs.put("riskLevel", "MEDIUM");\n    outputs.put("bmiCategory", "OVERWEIGHT");\n    outputs.put("message", "BMI 偏高，建议加费承保");\n    outputs.put("surchargeRate", 0.1);\nend\n\nrule "BMI_UNDERWRITING_OBESE"\nwhen\n    $f : RiskFact(bmi != null, bmi >= 28)\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.MANUAL_REVIEW;\n    hitRules.add("BMI_UNDERWRITING_OBESE");\n    outputs.put("riskLevel", "HIGH");\n    outputs.put("bmiCategory", "OBESE");\n    outputs.put("message", "BMI 过高，需人工核保");\nend',
 '{"logic":"AND","conditions":[{"field":"bmi","operator":"<","value":"24"},{"field":"age","operator":">=","value":"18"},{"field":"age","operator":"<=","value":"60"}]}',
 FALSE, 'admin', 1);

INSERT INTO re_rule_template (template_code, template_name, category, business_line, description, drl_template, visual_template, sensitive, owner, sort_order) VALUES
('AUTO_RISK_DRIVER_AGE', '车险驾驶员年龄风控', 'RISK_CONTROL', 'PROPERTY',
 '基于驾驶员年龄的车险风控规则模板',
 'package com.insurance.ruleengine\n\ndialect "java"\n\nglobal com.insurance.ruleengine.domain.model.DecisionType decisionType;\n\nglobal java.util.List<String> hitRules;\n\nglobal java.util.Map<String, Object> outputs;\n\nglobal StringBuilder auditLog;\n\nrule "AUTO_RISK_DRIVER_YOUNG"\nwhen\n    $f : RiskFact(driverAge != null, driverAge < 25)\n    $f.getDrivingYears() < 3\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.RATE_UP;\n    hitRules.add("AUTO_RISK_DRIVER_YOUNG");\n    outputs.put("riskLevel", "HIGH");\n    outputs.put("surchargeRate", 0.3);\n    outputs.put("message", "新手驾驶员，保费上浮 30%");\nend\n\nrule "AUTO_RISK_DRIVER_MIDDLE"\nwhen\n    $f : RiskFact(driverAge != null, driverAge >= 25, driverAge <= 60)\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.ACCEPT;\n    hitRules.add("AUTO_RISK_DRIVER_MIDDLE");\n    outputs.put("riskLevel", "LOW");\n    outputs.put("message", "驾驶员年龄正常，标准费率");\nend\n\nrule "AUTO_RISK_DRIVER_SENIOR"\nwhen\n    $f : RiskFact(driverAge != null, driverAge > 70)\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.MANUAL_REVIEW;\n    hitRules.add("AUTO_RISK_DRIVER_SENIOR");\n    outputs.put("riskLevel", "MEDIUM");\n    outputs.put("message", "高龄驾驶员，需人工审核");\nend',
 '{"logic":"AND","conditions":[{"field":"driverAge","operator":"<","value":"25"},{"field":"drivingYears","operator":"<","value":"3"}]}',
 FALSE, 'admin', 2);

INSERT INTO re_rule_template (template_code, template_name, category, business_line, description, drl_template, visual_template, sensitive, owner, sort_order) VALUES
('LIFE_PRICING_TERM', '寿险定期定价规则', 'PRICING', 'LIFE',
 '定期寿险定价规则模板，按年龄 + 保额 + 保障期限计算保费',
 'package com.insurance.ruleengine\n\ndialect "java"\n\nglobal com.insurance.ruleengine.domain.model.DecisionType decisionType;\n\nglobal java.util.List<String> hitRules;\n\nglobal java.util.Map<String, Object> outputs;\n\nglobal StringBuilder auditLog;\n\nrule "LIFE_PRICING_STANDARD"\nwhen\n    $f : PricingFact(age >= 18, age <= 60, coverage != null, coverage <= 1000000)\n    $f.getTermYears() <= 30\nthen\n    double base = $f.getAge() * 0.02 * $f.getCoverage() / 1000000;\n    double premium = base * ($f.getTermYears() / 30.0);\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.PRICE_ADJUST;\n    hitRules.add("LIFE_PRICING_STANDARD");\n    outputs.put("basePremium", premium);\n    outputs.put("totalPremium", premium);\n    outputs.put("priceLevel", "STANDARD");\nend\n\nrule "LIFE_PRICING_HIGH_COVERAGE"\nwhen\n    $f : PricingFact(coverage != null, coverage > 1000000)\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.PRICE_ADJUST;\n    hitRules.add("LIFE_PRICING_HIGH_COVERAGE");\n    outputs.put("priceLevel", "HIGH");\n    outputs.put("message", "高保额需额外核保审核");\nend',
 '{"logic":"AND","conditions":[{"field":"age","operator":">=","value":"18"},{"field":"age","operator":"<=","value":"60"},{"field":"coverage","operator":"<=","value":"1000000"}]}',
 FALSE, 'admin', 3);

INSERT INTO re_rule_template (template_code, template_name, category, business_line, description, drl_template, visual_template, sensitive, owner, sort_order) VALUES
('OCCUPATION_CLASSIFICATION', '职业类别分类规则', 'RISK_CONTROL', 'LIFE',
 '按职业类别划分风险等级，用于寿险核保分类',
 'package com.insurance.ruleengine\n\ndialect "java"\n\nglobal com.insurance.ruleengine.domain.model.DecisionType decisionType;\n\nglobal java.util.List<String> hitRules;\n\nglobal java.util.Map<String, Object> outputs;\n\nglobal StringBuilder auditLog;\n\nrule "OCCUPATION_CLASS_DESK"\nwhen\n    $f : RiskFact(occupationCode != null, occupationCode.startsWith("DESK_"))\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.ACCEPT;\n    hitRules.add("OCCUPATION_CLASS_DESK");\n    outputs.put("occupationClass", "CLASS_1");\n    outputs.put("riskLevel", "LOW");\n    outputs.put("message", "室内办公，标准体承保");\nend\n\nrule "OCCUPATION_CLASS_LIGHT"\nwhen\n    $f : RiskFact(occupationCode != null, occupationCode.startsWith("LIGHT_"))\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.ACCEPT;\n    hitRules.add("OCCUPATION_CLASS_LIGHT");\n    outputs.put("occupationClass", "CLASS_2");\n    outputs.put("riskLevel", "LOW");\n    outputs.put("message", "轻体力劳动，标准体承保");\nend\n\nrule "OCCUPATION_CLASS_HEAVY"\nwhen\n    $f : RiskFact(occupationCode != null, occupationCode.startsWith("HEAVY_"))\nthen\n    decisionType = com.insurance.ruleengine.domain.model.DecisionType.MANUAL_REVIEW;\n    hitRules.add("OCCUPATION_CLASS_HEAVY");\n    outputs.put("occupationClass", "CLASS_4");\n    outputs.put("riskLevel", "HIGH");\n    outputs.put("message", "高危职业，需人工审核");\nend',
 '{"logic":"AND","conditions":[{"field":"occupationCode","operator":"startsWith","value":"DESK_"}]}',
 FALSE, 'admin', 4);
