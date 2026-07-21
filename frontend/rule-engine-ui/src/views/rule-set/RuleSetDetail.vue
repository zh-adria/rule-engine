<template>
  <div class="rule-set-detail">
    <header class="rule-set-detail__mast">
      <div class="rule-set-detail__bread">
        <button @click="$router.push('/rule-sets')">规则编排</button>
        <ChevronRight :size="12" />
        <span>{{ setForm.setCode || '新建规则集' }}</span>
      </div>
      <h1 class="rule-set-detail__hed">
        <code class="re-text-code">{{ setForm.setCode || 'NEW_RULESET' }}</code>
        <span class="rule-set-detail__name">{{ setForm.setName }}</span>
      </h1>
      <div class="rule-set-detail__meta">
        <span v-if="setForm.owner" class="rule-set-detail__owner">by {{ setForm.owner }}</span>
        <span class="rule-set-detail__steps-count">
          <Layers :size="12" />{{ setForm.steps.length }} 步
        </span>
      </div>
      <button class="rule-set-detail__back" @click="$router.push('/rule-sets')">
        <ArrowLeft :size="14" />
        <span>返回</span>
      </button>
    </header>

    <div class="rule-set-detail__grid">
      <!-- 左:配置 + 编排 -->
      <div>
        <section class="panel">
          <p class="panel__kicker">CONFIG · 规则集配置</p>
          <el-form label-position="top" :model="setForm">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="规则集编码">
                  <el-input v-model="setForm.setCode" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="规则集名称">
                  <el-input v-model="setForm.setName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="负责人">
                  <el-input v-model="setForm.owner" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述">
              <el-input v-model="setForm.description" />
            </el-form-item>
          </el-form>
        </section>

        <!-- 编排流程 -->
        <section class="panel panel--flow">
          <header class="panel__flow-head">
            <p class="panel__kicker">FLOW · 编排步骤</p>
            <button class="panel__add-step" @click="addStep">
              <Plus :size="14" />
              <span>添加步骤</span>
            </button>
          </header>

          <!-- 时间轴流程 -->
          <div class="flow" v-if="setForm.steps.length">
            <div
              v-for="(step, index) in setForm.steps"
              :key="index"
              class="flow-step"
            >
              <div class="flow-step__timeline">
                <div class="flow-step__node">
                  <span>{{ index + 1 }}</span>
                </div>
                <div
                  v-if="index < setForm.steps.length - 1"
                  class="flow-step__link"
                  :class="`is-${step.mode.toLowerCase()}`"
                >
                  <ArrowDown :size="14" />
                  <span class="flow-step__mode">
                    {{ step.mode === 'PARALLEL' ? '下一并行' : '下一串行' }}
                  </span>
                </div>
              </div>
              <div class="flow-step__card">
                <el-row :gutter="12" align="top">
                  <el-col :span="10">
                    <el-form-item label="规则编码" size="small">
                      <el-select
                        v-model="step.ruleCode"
                        placeholder="选择规则"
                        filterable
                        style="width: 100%"
                        size="small"
                      >
                        <el-option
                          v-for="r in rulesStore.rules"
                          :key="r.ruleCode"
                          :label="r.ruleCode"
                          :value="r.ruleCode"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="执行模式" size="small">
                      <el-select v-model="step.mode" size="small" style="width: 100%">
                        <el-option label="串行 ↓" value="SERIAL" />
                        <el-option label="平行 ⇋" value="PARALLEL" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item label="版本" size="small">
                      <el-input-number
                        v-model="step.ruleVersion"
                        :min="1"
                        size="small"
                        controls-position="right"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="4" class="flow-step__actions">
                    <el-form-item label="中止策略" size="small">
                      <el-checkbox v-model="step.stopOnDecline">
                        命中 DECLINE 中止
                      </el-checkbox>
                    </el-form-item>
                    <button class="flow-step__remove" @click="removeStep(index)" title="删除">
                      <Trash2 :size="14" />
                    </button>
                  </el-col>
                </el-row>
              </div>
            </div>
          </div>

          <!-- empty -->
          <div v-else class="flow-empty">
            <Workflow :size="36" class="flow-empty__icon" />
            <h3 class="flow-empty__title">暂无编排步骤</h3>
            <p class="flow-empty__desc">点击「添加步骤」开始编排。上层步骤的结果会作为输入传入下层。</p>
            <button class="re-btn re-btn--primary" @click="addStep">
              <Plus :size="14" />
              <span>添加步骤</span>
            </button>
          </div>

          <footer class="panel__save">
            <el-button v-if="isEditing" :icon="Trash2" type="danger" @click="deleteRuleSet">删除规则集</el-button>
            <el-button :icon="Save" type="primary" @click="saveRuleSet">{{ isEditing ? '更新规则集' : '保存规则集' }}</el-button>
          </footer>
        </section>
      </div>

      <!-- 右:测试 -->
      <div>
        <section class="panel panel--test">
          <p class="panel__kicker">TEST · 执行事实</p>
          <el-input
            v-model="testFactsText"
            type="textarea"
            :rows="10"
            spellcheck="false"
          />
          <div class="panel__run">
            <el-button :icon="Play" type="primary" :disabled="!setCode" @click="runSetTest">
              执行规则集
            </el-button>
          </div>
        </section>

        <section class="panel panel--code">
          <p class="panel__kicker">RESULT · 执行结果</p>
          <pre class="result-pre">{{ formattedResult }}</pre>

          <div v-if="setResult?.stepResults" class="step-results">
            <h4 class="step-results__title">步骤明细</h4>
            <el-table :data="setResult.stepResults" size="small" max-height="240">
              <el-table-column prop="stepOrder" label="步骤" width="60" />
              <el-table-column prop="ruleCode" label="规则" min-width="180" />
              <el-table-column prop="version" label="版本" width="70" />
              <el-table-column label="决策" width="130">
                <template #default="{ row }">
                  <StatusTag v-if="row.decision" :status="row.decision" />
                  <span v-else class="re-text-muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="跳过" width="70">
                <template #default="{ row }">
                  <span :class="row.skipped ? 're-tag re-tag--neutral' : 're-tag re-tag--success'">
                    {{ row.skipped ? '是' : '否' }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Save, Play, Trash2, ArrowLeft, ArrowDown, ChevronRight, Layers, Workflow
} from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'
import { useRulesStore } from '../../stores/rules'
import { getRuleSet } from '../../api/rules'
import StatusTag from '../../components/common/StatusTag.vue'

const route = useRoute()
const authStore = useAuthStore()
const rulesStore = useRulesStore()

const setCode = computed(() => route.params.setCode)
const isEditing = computed(() => !!setCode.value && setCode.value !== 'new')
const setResult = computed(() => rulesStore.setResult)
const formattedResult = computed(() =>
  setResult.value ? JSON.stringify(setResult.value, null, 2) : '// 单击"执行规则集"后显示结果'
)

const setForm = reactive({
  setCode: '',
  setName: '',
  description: '',
  owner: '',
  steps: []
})

const testFactsText = ref(
  JSON.stringify({
    productCode: 'CI2026',
    bmi: 33.2,
    hasDiabetes: false,
    occupationClass: 2,
    sumInsured: 500000
  }, null, 2)
)

function addStep() {
  setForm.steps.push({
    ruleCode: '',
    ruleVersion: null,
    mode: 'SERIAL',
    stopOnDecline: true
  })
}

function removeStep(index) {
  setForm.steps.splice(index, 1)
}

async function saveRuleSet() {
  try {
    if (isEditing.value) {
      await rulesStore.updateRuleSetAction(setCode.value, setForm)
      ElMessage.success(`规则集 ${setForm.setCode} 已更新`)
    } else {
      await rulesStore.saveRuleSet(setForm)
      ElMessage.success(`规则集 ${setForm.setCode} 已保存`)
    }
  } catch (e) {
    ElMessage.error(`保存失败:${e.message || e}`)
  }
}

async function deleteRuleSet() {
  if (!setCode.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除规则集 "${setForm.setName || setForm.setCode}" 吗? 此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await rulesStore.deleteRuleSetAction(setCode.value)
    ElMessage.success('规则集已删除')
    router.push('/rule-sets')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

async function runSetTest() {
  try {
    const facts = JSON.parse(testFactsText.value)
    await rulesStore.runSetTest(setCode.value, facts, authStore.username)
  } catch (e) {
    ElMessage.error(`执行失败:${e.message || e}`)
  }
}

onMounted(async () => {
  await rulesStore.loadRules()
  if (setCode.value && setCode.value !== 'new') {
    try {
      const rs = await getRuleSet(setCode.value)
      rulesStore.selectRuleSet(rs)
      setForm.setCode = rs.setCode
      setForm.setName = rs.setName
      setForm.description = rs.description || ''
      setForm.owner = rs.owner || ''
      setForm.steps = (rs.steps || []).map((s) => ({
        ruleCode: s.ruleCode,
        ruleVersion: s.ruleVersion,
        mode: s.mode || 'SERIAL',
        stopOnDecline: s.stopOnDecline ?? true
      }))
    } catch (e) {
      ElMessage.error('加载规则集失败,请刷新重试')
    }
  }
})
</script>

<style scoped>
/* ===========================================================
   RULE SET DETAIL  —  流程编排风
   · 时间轴:垂直节点 + 卡片 + 模式链接
   · 每一节点是独立卡片,可拖拽删除
   · 执行结果用暗色代码风格
   =========================================================== */

.rule-set-detail {
  padding-bottom: var(--sp-8);
}

/* ---------- mast ---------- */
.rule-set-detail__mast {
  position: relative;
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 2px solid var(--color-neutral-900);
}
.rule-set-detail__bread {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  margin-bottom: var(--sp-2);
}
.rule-set-detail__bread button {
  all: unset;
  cursor: pointer;
  color: var(--color-primary-600);
  font-weight: 500;
}
.rule-set-detail__bread button:hover {
  text-decoration: underline;
}
.rule-set-detail__hed {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
}
.rule-set-detail__hed code {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 600;
  color: var(--color-neutral-900);
  letter-spacing: var(--tracking-tight);
}
.rule-set-detail__name {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 500;
  font-style: italic;
  color: var(--color-neutral-500);
}
.rule-set-detail__meta {
  margin-top: var(--sp-2);
  display: flex;
  gap: var(--sp-3);
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
}
.rule-set-detail__steps-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary-700);
  font-weight: 500;
}
.rule-set-detail__back {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fs-sm);
  color: var(--color-neutral-600);
  padding: 4px 12px;
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition: all var(--dur-2) var(--ease);
}
.rule-set-detail__back:hover {
  background: var(--color-neutral-100);
  color: var(--color-neutral-900);
}

/* ---------- grid ---------- */
.rule-set-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(380px, 0.8fr);
  gap: var(--sp-6);
  align-items: start;
}
@media (max-width: 1100px) {
  .rule-set-detail__grid { grid-template-columns: 1fr; }
}

/* ---------- panel ---------- */
.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  box-shadow: var(--shadow-sm);
}
.panel + .panel { margin-top: var(--sp-5); }
.panel__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-neutral-500);
  text-transform: uppercase;
  margin: 0 0 var(--sp-4);
  padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
}

.panel--code {
  background: #0b1020;
  color: #e0e7ff;
  border: 1px solid rgba(99,102,241,0.3);
}
.panel--code .panel__kicker {
  color: rgba(224,231,255,0.5);
  border-bottom-color: rgba(255,255,255,0.1);
}

.panel--test { background: var(--color-neutral-50); }
.panel__run {
  margin-top: var(--sp-4);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--color-neutral-200);
}

/* ---------- flow ---------- */
.panel__flow-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--sp-5);
  padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
}
.panel__flow-head .panel__kicker {
  margin: 0;
  padding: 0;
  border-bottom: none;
}
.panel__add-step {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 4px 12px;
  background: var(--color-primary-600);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-size: var(--fs-sm);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--dur-2) var(--ease);
}
.panel__add-step:hover {
  background: var(--color-primary-700);
}

.flow {
  display: flex;
  flex-direction: column;
}
.flow-step {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: var(--sp-3);
  position: relative;
}
.flow-step__timeline {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.flow-step__node {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary-600);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}
.flow-step__link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 60px;
  width: 2px;
  background: var(--color-neutral-200);
  padding: 6px 0;
  margin-top: var(--sp-2);
  position: relative;
}
.flow-step__link.is-parallel {
  background: linear-gradient(180deg, var(--color-neutral-200), var(--color-warning-bg));
}
.flow-step__link.is-serial {
  background: linear-gradient(180deg, var(--color-neutral-200), var(--color-primary-200));
}
.flow-step__link > svg {
  position: absolute;
  bottom: 0;
  color: var(--color-neutral-500);
  background: var(--color-surface);
  padding: 2px 0;
}
.flow-step__mode {
  position: absolute;
  top: 50%;
  left: 16px;
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-neutral-500);
  background: var(--color-surface);
  padding: 1px 6px;
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-full);
  transform: translateY(-50%);
}
.flow-step__link.is-parallel .flow-step__mode {
  color: var(--color-warning-fg);
  border-color: var(--color-warning);
  background: #fffbeb;
}
.flow-step__link.is-serial .flow-step__mode {
  color: var(--color-primary-700);
  border-color: var(--color-primary-300);
  background: var(--color-primary-50);
}

.flow-step__card {
  border: 1px solid var(--color-neutral-200);
  border-left: 3px solid var(--color-primary-500);
  border-radius: var(--radius-md);
  padding: var(--sp-4);
  background: var(--color-surface);
  margin-bottom: var(--sp-3);
}
.flow-step__actions {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: var(--sp-2);
  padding-bottom: 22px;
}
.flow-step__remove {
  all: unset;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-neutral-200);
  background: var(--color-surface);
  color: var(--color-neutral-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--dur-2) var(--ease);
}
.flow-step__remove:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger);
  color: var(--color-danger);
}

/* 步骤结果 */
.step-results {
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px solid rgba(255,255,255,0.1);
}
.step-results__title {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: rgba(224,231,255,0.7);
  margin: 0 0 var(--sp-3);
}

.flow-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--sp-10) var(--sp-4);
  gap: var(--sp-3);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.flow-empty__icon { color: var(--color-neutral-300); }
.flow-empty__title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-700);
}
.flow-empty__desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
  max-width: 460px;
}
.flow-empty .re-btn--primary {
  margin-top: var(--sp-2);
}

.panel__save {
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--color-neutral-200);
}

.result-pre {
  margin: 0;
  padding: var(--sp-4);
  background: rgba(0,0,0,0.3);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.55;
  color: #c7d2fe;
  max-height: 320px;
  overflow: auto;
  border: 1px solid rgba(99,102,241,0.2);
}
</style>
