/**
 * DRL Generator - 将可视化条件模型转换为 Drools Rule Language (DRL)
 * 以及从 DRL 反向解析为可视化条件
 */

/**
 * 从可视化条件生成 DRL 规则内容
 * @param {string} packageName - DRL 包名
 * @param {string} ruleName - 规则名称
 * @param {Array} conditions - 可视化条件数组
 * @param {Object} action - 规则动作配置
 * @returns {string} DRL 内容
 */
export function generateDrl(packageName, ruleName, conditions, action = {}) {
  const {
    decision = 'MANUAL_REVIEW',
    outputKey = 'conclusion',
    outputValue = '规则命中',
    salience = 80
  } = action

  // Normalize and collect when clauses from all conditions (including groups)
  function collectClauses(items, topLogic) {
    const clauses = []
    for (const item of items) {
      if (item.type === 'group') {
        const groupClauses = collectGroupClauses(item)
        if (groupClauses.length > 0) {
          if (item.conditions.length === 1) {
            clauses.push(...groupClauses)
          } else {
            // Wrap group in parentheses for OR logic
            if (item.logic === 'OR' && clauses.length > 0) {
              clauses.push('( ' + groupClauses.join(' ||\n') + ' )')
            } else {
              clauses.push(...groupClauses)
            }
          }
        }
      } else {
        const clause = toWhenClause(item)
        if (clause) clauses.push(clause)
      }
    }
    return clauses
  }

  function collectGroupClauses(group) {
    const clauses = []
    for (const sub of group.conditions) {
      const clause = toWhenClause(sub)
      if (clause) clauses.push(clause)
    }
    return clauses
  }

  const allClauses = collectClauses(conditions, 'AND')
  if (allClauses.length === 0) return ''
  if (whenClauses.length === 0) {
    return ''
  }

  const factsPattern = `  $facts: Map(\n${allClauses.join(',\n')})`
  const resultPattern = `  $result: ExecutionResult(decision != DecisionType.DECLINE)`

  return `package ${packageName}

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

rule "${ruleName}"
salience ${salience}
when
${factsPattern}
${resultPattern}
then
  $result.setDecision(DecisionType.${decision});
  $result.getHitRules().add("${ruleName}");
  $result.getOutputs().put("${outputKey}", "${outputValue}");
end
`
}

/**
 * 将单个可视化条件转换为 DRL when 子句
 */
function toWhenClause(condition) {
  const { field, operator, value, betweenLo, betweenHi } = condition
  if (!field || !operator) return null

  // For BETWEEN, compose value from betweenLo/betweenHi
  let effectiveValue = value
  if (operator === 'between' && betweenLo != null && betweenHi != null) {
    effectiveValue = `${betweenLo},${betweenHi}`
  }

  if (operator !== 'isNull' && operator !== 'isNotNull' && (effectiveValue === undefined || effectiveValue === '')) {
    return null
  }

  const accessor = `this["${field}"]`

  // 根据值类型推断转换
  const numValue = Number(value)
  const isNumeric = !isNaN(numValue) && value !== ''
  const isBoolean = value === 'true' || value === 'false'

  switch (operator) {
    case '==':
      if (isBoolean(effectiveValue)) {
        return `              ${accessor} == ${effectiveValue}`
      }
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() == ${numValue}`
      }
      return `              ${accessor} == "${effectiveValue}"`

    case '!=':
      if (isBoolean(effectiveValue)) {
        return `              ${accessor} != ${effectiveValue}`
      }
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() != ${numValue}`
      }
      return `              ${accessor} != "${effectiveValue}"`

    case '>':
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() > ${numValue}`
      }
      return null

    case '>=':
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() >= ${numValue}`
      }
      return null

    case '<':
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() < ${numValue}`
      }
      return null

    case '<=':
      if (isNumeric) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() <= ${numValue}`
      }
      return null

    case 'contains':
      return `              ${accessor} != null,\n              ((String)${accessor}).contains("${effectiveValue}")`

    case 'matches':
      return `              ${accessor} != null,\n              ((String)${accessor})).matches("${effectiveValue}")`

    case 'isNull':
      return `              ${accessor} == null`

    case 'isNotNull':
      return `              ${accessor} != null`

    case 'between': {
      const parts = String(effectiveValue).split(',').map(s => s.trim())
      const lo = parts[0] || '0'
      const hi = parts[1] || '0'
      const loNum = Number(lo)
      const hiNum = Number(hi)
      if (!isNaN(loNum) && !isNaN(hiNum)) {
        return `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() >= ${loNum},\n              ((Number)${accessor}).doubleValue() <= ${hiNum}`
      }
      return null
    }

    case 'in': {
      const vals = String(effectiveValue).split(',').map(s => s.trim()).filter(Boolean)
      if (vals.length === 0) return null
      const clauses = vals.map(v => isNaN(Number(v)) ? `              ${accessor} == "${v}"` : `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() == ${Number(v)}`).join(' ||\n')
      return `              ${accessor} != null,\n${clauses}`
    }

    case 'notIn': {
      const vals = String(effectiveValue).split(',').map(s => s.trim()).filter(Boolean)
      if (vals.length === 0) return null
      const clauses = vals.map(v => isNaN(Number(v)) ? `              ${accessor} != "${v}"` : `              ${accessor} != null,\n              ((Number)${accessor}).doubleValue() != ${Number(v)}`).join(' &&\n')
      return `              ${accessor} != null,\n${clauses}`
    }

    default:
      return null
  }
}

/**
 * 从 DRL 内容反向解析为可视化条件（尽力解析，复杂规则返回空数组）
 * @param {string} drlContent - DRL 内容
 * @returns {Array} 可视化条件数组
 */
export function parseDrlToConditions(drlContent) {
  if (!drlContent) return []

  const conditions = []

  // 匹配 this["field"] == "value" 或 this["field"] == number
  const stringEqPattern = /this\["(\w+)"\]\s*==\s*"([^"]+)"/g
  const numEqPattern = /\(\(Number\)this\["(\w+)"\]\)\.doubleValue\(\)\s*(==|>=|<=|>|<|!=)\s*([\d.]+)/g
  const nullCheckPattern = /this\["(\w+)"\]\s*==\s*null/g
  const notNullCheckPattern = /this\["(\w+)"\]\s*!=\s*null/g

  let match

  // 解析 IS NULL
  while ((match = nullCheckPattern.exec(drlContent)) !== null) {
    conditions.push({ field: match[1], operator: 'isNull', value: '' })
  }

  // 解析 IS NOT NULL
  while ((match = notNullCheckPattern.exec(drlContent)) !== null) {
    conditions.push({ field: match[1], operator: 'isNotNull', value: '' })
  }

  // 解析字符串等值条件
  while ((match = stringEqPattern.exec(drlContent)) !== null) {
    conditions.push({
      field: match[1],
      operator: '==',
      value: match[2]
    })
  }

  // 解析数值比较条件
  while ((match = numEqPattern.exec(drlContent)) !== null) {
    conditions.push({
      field: match[1],
      operator: match[2],
      value: match[3]
    })
  }

  return conditions
}

/**
 * 从 DRL 内容解析动作配置
 * @param {string} drlContent - DRL 内容
 * @returns {Object} 动作配置
 */
export function parseDrlToAction(drlContent) {
  if (!drlContent) return {}

  const decisionMatch = drlContent.match(/setDecision\(DecisionType\.(\w+)\)/)
  const outputKeyMatch = drlContent.match(/getOutputs\(\)\.put\("(\w+)"/)
  const outputValueMatch = drlContent.match(/getOutputs\(\)\.put\("\w+",\s*"([^"]+)"\)/)
  const salienceMatch = drlContent.match(/salience\s+(\d+)/)
  const ruleNameMatch = drlContent.match(/rule\s+"([^"]+)"/)

  return {
    decision: decisionMatch ? decisionMatch[1] : 'MANUAL_REVIEW',
    outputKey: outputKeyMatch ? outputKeyMatch[1] : 'conclusion',
    outputValue: outputValueMatch ? outputValueMatch[1] : '规则命中',
    salience: salienceMatch ? parseInt(salienceMatch[1]) : 80,
    ruleName: ruleNameMatch ? ruleNameMatch[1] : ''
  }
}

/**
 * 支持的条件操作符列表
 */
export const OPERATORS = [
  { label: '等于', value: '==', types: ['string', 'number', 'boolean'], desc: '严格相等比较' },
  { label: '不等于', value: '!=', types: ['string', 'number', 'boolean'], desc: '不相等比较' },
  { label: '大于', value: '>', types: ['number'], desc: '左侧值大于右侧值' },
  { label: '大于等于', value: '>=', types: ['number'], desc: '左侧值大于或等于右侧值' },
  { label: '小于', value: '<', types: ['number'], desc: '左侧值小于右侧值' },
  { label: '小于等于', value: '<=', types: ['number'], desc: '左侧值小于或等于右侧值' },
  { label: '包含', value: 'contains', types: ['string'], desc: '左侧字符串包含右侧子串' },
  { label: '正则匹配', value: 'matches', types: ['string'], desc: '左侧字符串匹配右侧正则表达式' },
  { label: '为空', value: 'isNull', types: ['string', 'number', 'boolean'], desc: '字段值为空（null 或空字符串）' },
  { label: '非空', value: 'isNotNull', types: ['string', 'number', 'boolean'], desc: '字段值不为空' },
  { label: '介于', value: 'between', types: ['number'], desc: '数值在两个边界之间（含边界）' },
  { label: '包含于', value: 'in', types: ['string', 'number'], desc: '字段值在给定列表中' },
  { label: '不包含于', value: 'notIn', types: ['string', 'number'], desc: '字段值不在给定列表中' }
]

/**
 * 支持的规则字段
 */
export const INSURANCE_FIELDS = [
  { label: '产品编码', value: 'productCode', type: 'string', desc: '产品唯一编码' },
  { label: 'BMI', value: 'bmi', type: 'number', desc: '身体质量指数 = 体重(kg) / 身高(m)²' },
  { label: '职业类别', value: 'occupationClass', type: 'number', desc: '1-6类职业，数字越大风险越高' },
  { label: '是否黑名单', value: 'blacklisted', type: 'boolean', desc: '是否命中黑名单（欺诈/高风险客户）' },
  { label: '保额', value: 'sumInsured', type: 'number', desc: '保额（元）' },
  { label: '年龄', value: 'age', type: 'number', desc: '年龄（周岁）' },
  { label: '性别', value: 'gender', type: 'string', desc: 'MALE=男, FEMALE=女' },
  { label: '是否有糖尿病', value: 'hasDiabetes', type: 'boolean', desc: '是否患有糖尿病' },
  { label: '是否有高血压', value: 'hasHypertension', type: 'boolean', desc: '是否患有高血压' },
  { label: '吸烟状态', value: 'smokingStatus', type: 'string', desc: 'NEVER=从不, FORMER=已戒烟, CURRENT=当前吸烟' },
  { label: '状态', value: 'policyStatus', type: 'string', desc: 'NEW=新建, RENEW=续期, ENDORSE=批改' },
  { label: '渠道编码', value: 'channelCode', type: 'string', desc: 'AGENCY=代理, DIRECT=直销, ONLINE=线上' },
  { label: '地区编码', value: 'regionCode', type: 'string', desc: '6位行政区划代码，如 110000=北京' }
]

/**
 * 决策类型选项
 */
export const DECISION_TYPES = [
  { label: '通过', value: 'ACCEPT' },
  { label: '人工审核', value: 'MANUAL_REVIEW' },
  { label: '拒绝', value: 'DECLINE' },
  { label: '加费', value: 'RATE_UP' },
  { label: '黑名单命中', value: 'BLACKLIST_HIT' },
  { label: '价格调整', value: 'PRICE_ADJUST' }
]
