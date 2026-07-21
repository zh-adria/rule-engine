<template>
  <div class="condition-editor">
    <header class="condition-editor__header">
      <span class="condition-editor__title">
        <Workflow :size="16" style="margin-right: 6px; vertical-align: -2px" />
        可视化条件
      </span>
      <div class="condition-editor__actions">
        <el-radio-group v-model="logicLocal" size="small" @change="emitLogic">
          <el-radio-button value="AND">全部满足(AND)</el-radio-button>
          <el-radio-button value="OR">任一满足(OR)</el-radio-button>
        </el-radio-group>
        <el-button :icon="Plus" size="small" @click="addCondition">
          添加条件
        </el-button>
        <el-button :icon="FolderPlus" size="small" @click="addGroup" type="default">
          分组
        </el-button>
      </div>
    </header>

    <div
      v-if="conditions.length === 0"
      class="condition-editor__empty"
    >
      <Workflow :size="20" />
      <p>暂无条件</p>
      <p class="condition-editor__empty-hint">
        点击"添加条件"创建规则，或点击"分组"创建逻辑组 (A AND B) OR C
      </p>
    </div>

    <div v-else class="condition-editor__list">
      <template v-for="(item, index) in localConditions" :key="itemKey(item, index)">
        <!-- 简单条件行 -->
        <div
          v-if="item.type !== 'group'"
          class="condition-row"
          :class="{ 'condition-row--drag-over': dragOverIndex === index && dragSourceIndex !== index }"
          draggable="true"
          @dragstart="onDragStart($event, index)"
          @dragover.prevent="onDragOver($event, index)"
          @dragleave="onDragLeave"
          @drop="onDrop($event, index)"
          @dragend="onDragEnd"
        >
          <span class="condition-row__drag" title="拖拽排序">⋮⋮</span>
          <span class="condition-row__index">{{ index + 1 }}</span>
          <el-select v-model="item.field" placeholder="字段" size="small" class="cond-field" @change="onFieldChange(item)">
            <el-option
              v-for="f in fields"
              :key="f.value"
              :label="f.label"
              :value="f.value"
              :title="f.desc || ''"
            />
          </el-select>
          <el-select v-model="item.operator" placeholder="操作符" size="small" class="cond-op" @change="onOperatorChange(item)">
            <el-option
              v-for="op in operatorsForField(item.field)"
              :key="op.value"
              :label="op.label"
              :value="op.value"
              :title="op.desc || ''"
            />
          </el-select>
          <template v-if="item.operator === 'between'">
            <el-input-number v-model="item.betweenLo" :min="null" :precision="2" size="small" placeholder="最小" class="cond-val" />
            <span class="cond-sep">~</span>
            <el-input-number v-model="item.betweenHi" :min="null" :precision="2" size="small" placeholder="最大" class="cond-val" />
          </template>
          <el-input
            v-else-if="item.operator === 'in' || item.operator === 'notIn'"
            v-model="item.value"
            placeholder="val1,val2"
            size="small"
            class="cond-val"
          />
          <span v-else-if="item.operator === 'isNull' || item.operator === 'isNotNull'" class="cond-hint">无需值</span>
          <el-input v-else v-model="item.value" placeholder="值" size="small" class="cond-val" />
          <el-button :icon="Trash2" circle size="small" @click="removeItem(index)" class="cond-del" />
        </div>

        <!-- 分组 -->
        <div
          v-else
          class="condition-group"
          draggable="true"
          @dragstart="onDragStart($event, index)"
          @dragover.prevent="onDragOver($event, index)"
          @dragleave="onDragLeave"
          @drop="onDrop($event, index)"
          @dragend="onDragEnd"
        >
          <div class="condition-group__header">
            <span class="condition-row__drag" title="拖拽排序">⋮⋮</span>
            <el-input
              v-model="item.groupName"
              size="small"
              class="cond-group-name"
              placeholder="分组名称"
              maxlength="30"
            />
            <el-radio-group v-model="item.logic" size="small">
              <el-radio-button value="AND">AND</el-radio-button>
              <el-radio-button value="OR">OR</el-radio-button>
            </el-radio-group>
            <div style="flex: 1" />
            <el-button size="small" @click="addConditionToGroup(item)">
              <Plus :size="12" style="margin-right: 3px" /> 条件
            </el-button>
            <el-button :icon="Trash2" circle size="small" type="danger" @click="removeItem(index)" />
          </div>
          <div class="condition-group__body">
            <div
              v-for="(sub, subIdx) in item.conditions"
              :key="subKey(item, index, subIdx)"
              class="condition-row condition-row--indent"
              draggable="true"
              @dragstart="onGroupDragStart($event, index, subIdx)"
              @dragover.prevent="onGroupDragOver($event, index, subIdx)"
              @dragleave="onGroupDragLeave"
              @drop="onGroupDrop($event, index, subIdx)"
              @dragend="onDragEnd"
            >
              <span class="condition-row__drag" title="拖拽排序">⋮⋮</span>
              <span class="condition-row__index">{{ subIdx + 1 }}</span>
              <el-select v-model="sub.field" placeholder="字段" size="small" class="cond-field" @change="onFieldChange(sub)">
                <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" :title="f.desc || ''" />
              </el-select>
              <el-select v-model="sub.operator" placeholder="操作符" size="small" class="cond-op" @change="onOperatorChange(sub)">
                <el-option v-for="op in operatorsForField(sub.field)" :key="op.value" :label="op.label" :value="op.value" :title="op.desc || ''" />
              </el-select>
              <template v-if="sub.operator === 'between'">
                <el-input-number v-model="sub.betweenLo" :min="null" :precision="2" size="small" placeholder="最小" class="cond-val" />
                <span class="cond-sep">~</span>
                <el-input-number v-model="sub.betweenHi" :min="null" :precision="2" size="small" placeholder="最大" class="cond-val" />
              </template>
              <el-input v-else-if="sub.operator === 'in' || sub.operator === 'notIn'" v-model="sub.value" placeholder="val1,val2" size="small" class="cond-val" />
              <span v-else-if="sub.operator === 'isNull' || sub.operator === 'isNotNull'" class="cond-hint">无需值</span>
              <el-input v-else v-model="sub.value" placeholder="值" size="small" class="cond-val" />
              <el-button :icon="Trash2" circle size="small" @click="removeConditionFromGroup(item, subIdx)" class="cond-del" />
            </div>
            <div v-if="item.conditions.length === 0" class="condition-group__empty">
              分组内暂无条件，点击"添加条件"
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { Plus, Trash2, Workflow, FolderPlus } from 'lucide-vue-next'
import { ref, watch, computed } from 'vue'
import { INSURANCE_FIELDS, OPERATORS } from '../../utils/drlGenerator'

const props = defineProps({
  conditions: { type: Array, required: true },
  logic: { type: String, default: 'AND' },
  businessLine: { type: String, default: '' }
})

const emit = defineEmits(['update:conditions', 'update:logic'])

const localConditions = ref([])

watch(() => props.conditions, (newVal) => {
  localConditions.value = newVal.map(c => {
    if (c.type === 'group') {
      return {
        ...c,
        groupName: c.groupName || `分组 ${localConditions.value.filter(x => x.type === 'group').length + 1}`,
        conditions: c.conditions.map(s => ({ ...s }))
      }
    }
    return { ...c }
  })
}, { immediate: true, deep: true })

watch(localConditions, (newVal) => {
  emit('update:conditions', newVal.map(c =>
    c.type === 'group'
      ? { ...c, conditions: c.conditions.map(s => ({ ...s })) }
      : { ...c }
  ))
}, { deep: true })

const fields = computed(() => INSURANCE_FIELDS)

const customFields = ref([])

watch(() => props.businessLine, async (bizLine) => {
  if (!bizLine) return
  try {
    const { listCustomFields } = await import('../../api/rules')
    customFields.value = (await listCustomFields(bizLine)).map(f => ({
      label: f.fieldLabel,
      value: f.fieldCode,
      type: f.fieldType,
      desc: f.description || ''
    }))
  } catch (e) {
    customFields.value = []
  }
}, { immediate: true })

const allFields = computed(() => [...fields.value, ...customFields.value])

const logicLocal = ref(props.logic || 'AND')
watch(() => props.logic, (v) => { if (v) logicLocal.value = v })
watch(logicLocal, (v) => emit('update:logic', v))

function operatorsForField(field) {
  const f = allFields.value.find(f => f.value === field)
  if (!f) return OPERATORS
  return OPERATORS.filter(op => op.types.includes(f.type))
}

function newSimpleCondition() {
  return { type: 'simple', field: '', operator: '', value: '', betweenLo: null, betweenHi: null }
}

function onFieldChange(item) {
  item.operator = ''
  item.value = ''
  item.betweenLo = null
  item.betweenHi = null
}

function onOperatorChange(item) {
  item.value = ''
  item.betweenLo = null
  item.betweenHi = null
}

function addCondition() {
  localConditions.value = [...localConditions.value, newSimpleCondition()]
}

function addConditionToGroup(group) {
  group.conditions = [...group.conditions, newSimpleCondition()]
}

function addGroup() {
  const groupCount = localConditions.value.filter(c => c.type === 'group').length + 1
  localConditions.value = [...localConditions.value, {
    type: 'group',
    logic: 'AND',
    groupName: `分组 ${groupCount}`,
    conditions: [newSimpleCondition()]
  }]
}

function removeItem(index) {
  localConditions.value = localConditions.value.filter((_, i) => i !== index)
}

function removeConditionFromGroup(group, subIdx) {
  group.conditions = group.conditions.filter((_, i) => i !== subIdx)
}

// ---- Drag sorting (top-level items) ----
const dragSourceIndex = ref(-1)
const dragOverIndex = ref(-1)

function itemKey(item, index) {
  return `cond-${index}-${item.type}-${item.field || item.groupName || 'empty'}`
}

function subKey(group, gIdx, sIdx) {
  return `sub-${gIdx}-${sIdx}-${group.conditions[sIdx]?.field || 'empty'}`
}

function onDragStart(e, index) {
  dragSourceIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(index))
}

function onDragOver(e, index) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
  dragOverIndex.value = index
}

function onDragLeave() {
  dragOverIndex.value = -1
}

function onDrop(e, targetIndex) {
  e.preventDefault()
  const source = parseInt(e.dataTransfer.getData('text/plain'), 10)
  if (isNaN(source) || source === targetIndex) {
    dragSourceIndex.value = -1
    dragOverIndex.value = -1
    return
  }
  const arr = [...localConditions.value]
  const [moved] = arr.splice(source, 1)
  arr.splice(targetIndex, 0, moved)
  localConditions.value = arr
  dragSourceIndex.value = -1
  dragOverIndex.value = -1
}

function onDragEnd() {
  dragSourceIndex.value = -1
  dragOverIndex.value = -1
}

// ---- Drag sorting (items inside a group) ----
let groupDragSource = null // { groupIndex, subIndex }

function onGroupDragStart(e, groupIndex, subIndex) {
  groupDragSource = { groupIndex, subIndex }
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', `group:${groupIndex}:${subIndex}`)
}

function onGroupDragOver(e, groupIndex, subIndex) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

function onGroupDragLeave() {
  // noop
}

function onGroupDrop(e, targetGroupIndex, targetSubIndex) {
  e.preventDefault()
  if (!groupDragSource) return
  const { groupIndex: srcGroup, subIndex: srcSub } = groupDragSource
  if (srcGroup !== targetGroupIndex || srcSub === targetSubIndex) {
    groupDragSource = null
    return
  }
  const group = localConditions.value[srcGroup]
  if (!group || !group.conditions) return
  const arr = [...group.conditions]
  const [moved] = arr.splice(srcSub, 1)
  arr.splice(targetSubIndex, 0, moved)
  group.conditions = arr
  groupDragSource = null
}
</script>

<style scoped>
.condition-editor {
  margin-bottom: var(--sp-4);
}

.condition-editor__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-3);
  flex-wrap: wrap;
}
.condition-editor__title {
  font-size: var(--fs-sm);
  font-weight: var(--fw-semibold);
  color: var(--color-neutral-800);
  display: inline-flex;
  align-items: center;
}
.condition-editor__actions {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
}

.condition-editor__empty {
  padding: var(--sp-8) var(--sp-4);
  border: 1px dashed var(--color-neutral-300);
  border-radius: var(--radius-md);
  text-align: center;
  color: var(--color-neutral-400);
  font-size: var(--fs-sm);
}
.condition-editor__empty p {
  margin: var(--sp-2) 0 0;
}
.condition-editor__empty-hint {
  font-size: var(--fs-xs);
  color: var(--color-neutral-400);
}

.condition-editor__list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

/* ---- condition row ---- */
.condition-row {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: grab;
  transition: all var(--dur-2) var(--ease);
}
.condition-row:active {
  cursor: grabbing;
}
.condition-row--drag-over {
  border-color: var(--color-primary-400) !important;
  background: var(--color-primary-50) !important;
  box-shadow: inset 0 0 0 1px var(--color-primary-300);
}
.condition-row--indent {
  border-left: 3px solid var(--color-primary-300);
}
.condition-row__drag {
  cursor: grab;
  color: var(--color-neutral-400);
  font-size: 14px;
  letter-spacing: -2px;
  user-select: none;
  flex-shrink: 0;
  width: 16px;
  text-align: center;
  line-height: 1;
  transition: color var(--dur-2) var(--ease);
}
.condition-row__drag:hover {
  color: var(--color-primary-500);
}
.condition-row:active .condition-row__drag {
  cursor: grabbing;
}
.condition-row__index {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  font-size: var(--fs-xs);
  font-weight: var(--fw-semibold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cond-field { width: 140px; flex-shrink: 0; }
.cond-op { width: 110px; flex-shrink: 0; }
.cond-val { flex: 1; min-width: 80px; }
.cond-sep {
  color: var(--color-neutral-400);
  font-size: 12px;
  flex-shrink: 0;
}
.cond-hint {
  font-size: 11px;
  color: var(--color-neutral-400);
  white-space: nowrap;
  flex-shrink: 0;
}
.cond-del { flex-shrink: 0; }

/* ---- group ---- */
.condition-group {
  border: 1px solid var(--color-primary-200);
  border-radius: var(--radius-lg);
  background: var(--color-primary-50);
  overflow: hidden;
  cursor: grab;
  transition: all var(--dur-2) var(--ease);
}
.condition-group:active {
  cursor: grabbing;
}
.condition-group__header {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  background: var(--color-primary-100);
  border-bottom: 1px solid var(--color-primary-200);
}
.cond-group-name {
  width: 120px;
  flex-shrink: 0;
}
.condition-group__body {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  padding: var(--sp-3);
}
.condition-group__empty {
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--color-neutral-400);
  padding: var(--sp-4);
}
</style>
