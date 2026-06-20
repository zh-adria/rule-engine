<template>
  <Login v-if="!authenticated" @login-success="onLoginSuccess" />

  <main v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">保险规则平台</div>
      <el-menu default-active="rules">
        <el-menu-item index="rules">
          <Settings class="menu-icon" />
          <span>规则配置</span>
        </el-menu-item>
        <el-menu-item index="audit">
          <ShieldCheck class="menu-icon" />
          <span>审计留痕</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <span class="user-info">{{ username }}</span>
        <el-button text size="small" @click="logout">退出</el-button>
      </div>
    </aside>

    <section class="workspace">
      <header class="toolbar">
        <div>
          <h1>核保、风控与定价规则</h1>
          <p>创建、测试、发布、灰度和回滚保险规则版本</p>
        </div>
        <div class="publish-tools">
          <span class="version-pill">v{{ currentVersion || '-' }}</span>
          <el-input-number v-model="publishGrayPercent" :min="0" :max="100" :step="5" controls-position="right" />
          <el-button type="primary" :icon="Rocket" :disabled="!currentVersion" @click="publish">发布版本</el-button>
        </div>
      </header>

      <div class="content-grid">
        <section class="panel editor-panel">
          <div class="panel-title">规则元数据</div>
          <el-form label-position="top" :model="ruleForm">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="规则编码">
                  <el-input v-model="ruleForm.ruleCode" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="规则名称">
                  <el-input v-model="ruleForm.ruleName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="规则类型">
                  <el-select v-model="ruleForm.category">
                    <el-option label="核保规则" value="UNDERWRITING" />
                    <el-option label="风控规则" value="RISK_CONTROL" />
                    <el-option label="产品定价" value="PRODUCT_PRICING" />
                    <el-option label="佣金比例" value="COMMISSION" />
                    <el-option label="监管内置" value="REGULATORY" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="业务线">
                  <el-input v-model="ruleForm.businessLine" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="负责人">
                  <el-input v-model="ruleForm.owner" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="银保监监管引用">
                  <el-input v-model="ruleForm.regulatoryRef" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-checkbox v-model="ruleForm.sensitive">敏感规则加密存储</el-checkbox>
            </el-form-item>
          </el-form>

          <div class="panel-title row-title">
            <span>可视化条件</span>
            <el-button :icon="Plus" size="small" @click="addCondition">添加条件</el-button>
          </div>
          <div class="condition-list">
            <div v-for="(item, index) in visualModel.conditions" :key="index" class="condition-row">
              <el-select v-model="item.field">
                <el-option label="产品编码" value="productCode" />
                <el-option label="BMI" value="bmi" />
                <el-option label="职业类别" value="occupationClass" />
                <el-option label="是否黑名单" value="blacklisted" />
                <el-option label="保额" value="sumInsured" />
              </el-select>
              <el-select v-model="item.operator">
                <el-option label="等于" value="==" />
                <el-option label="大于等于" value=">=" />
                <el-option label="小于" value="<" />
              </el-select>
              <el-input v-model="item.value" />
              <el-button :icon="Trash2" circle @click="removeCondition(index)" />
            </div>
          </div>

          <div class="panel-title">DRL 编辑器</div>
          <el-input v-model="drlContent" type="textarea" :rows="16" spellcheck="false" />
          <div class="action-row">
            <el-button :icon="Save" type="primary" @click="saveRuleAndVersion">保存草稿</el-button>
            <el-button :icon="Play" @click="runTest">测试执行</el-button>
          </div>
        </section>

        <section class="panel result-panel">
          <div class="panel-title">测试事实</div>
          <el-input v-model="testFactsText" type="textarea" :rows="12" spellcheck="false" />
          <div class="panel-title">执行结果</div>
          <pre class="json-view">{{ formattedResult }}</pre>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Plus, Play, Rocket, Save, Settings, ShieldCheck, Trash2 } from 'lucide-vue-next'
import { createRule, createVersion, publishRule, testRule } from './api/rules'
import Login from './views/Login.vue'

const authenticated = ref(!!localStorage.getItem('token'))
const username = ref(localStorage.getItem('username') || '')

function onLoginSuccess() {
  authenticated.value = true
  username.value = localStorage.getItem('username') || ''
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  authenticated.value = false
  username.value = ''
}

const ruleForm = reactive({
  ruleCode: 'CI_UW_HEALTH_2026',
  ruleName: '重疾险健康告知核保规则',
  category: 'UNDERWRITING',
  businessLine: 'CRITICAL_ILLNESS',
  description: '重疾险健康告知、BMI、职业类别核保',
  sensitive: true,
  owner: 'underwriting-team',
  regulatoryRef: 'CBIRC-INSURANCE-SALES-TRACE'
})

const visualModel = reactive({
  conditions: [
    { field: 'productCode', operator: '==', value: 'CI2026' },
    { field: 'bmi', operator: '>=', value: '32' }
  ]
})

const drlContent = ref(`package insurance.underwriting

import java.util.Map;
import com.insurance.ruleengine.domain.model.ExecutionResult;
import com.insurance.ruleengine.domain.model.DecisionType;

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
`)

const testFactsText = ref(JSON.stringify({
  productCode: 'CI2026',
  bmi: 33.2,
  hasDiabetes: false,
  occupationClass: 2
}, null, 2))

const result = ref(null)
const currentVersion = ref(null)
const publishGrayPercent = ref(10)

const formattedResult = computed(() => result.value ? JSON.stringify(result.value, null, 2) : '{}')

function addCondition() {
  visualModel.conditions.push({ field: 'productCode', operator: '==', value: '' })
}

function removeCondition(index) {
  visualModel.conditions.splice(index, 1)
}

async function saveRuleAndVersion() {
  try {
    await createRule(ruleForm)
  } catch (error) {
    if (!String(error?.response?.data?.message || '').includes('already exists')) {
      throw error
    }
  }
  const saved = await createVersion(ruleForm.ruleCode, {
    drlContent: drlContent.value,
    visualModel: JSON.stringify(visualModel),
    createdBy: ruleForm.owner
  })
  currentVersion.value = saved.latestVersion || saved.currentVersion || currentVersion.value
  ElMessage.success(`规则草稿已保存，当前版本 v${currentVersion.value}`)
}

async function runTest() {
  if (!currentVersion.value) {
    ElMessage.warning('请先保存草稿生成版本')
    return
  }
  const facts = JSON.parse(testFactsText.value)
  result.value = await testRule(ruleForm.ruleCode, {
    ruleCode: ruleForm.ruleCode,
    version: currentVersion.value,
    scenario: 'UNDERWRITING_TEST',
    facts,
    operator: ruleForm.owner
  })
}

async function publish() {
  const published = await publishRule(ruleForm.ruleCode, {
    version: currentVersion.value,
    approvedBy: ruleForm.owner,
    grayPercent: publishGrayPercent.value
  })
  currentVersion.value = published.latestVersion || published.grayVersion || published.currentVersion || currentVersion.value
  ElMessage.success(`版本 v${currentVersion.value} 已发布，灰度比例 ${publishGrayPercent.value}%`)
}
</script>

<style scoped>
.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: #1d2738;
}
.user-info {
  font-size: 13px;
  color: #666;
}
.publish-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.version-pill {
  min-width: 48px;
  height: 32px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  color: #606266;
  background: #fff;
  font-size: 13px;
}
</style>
