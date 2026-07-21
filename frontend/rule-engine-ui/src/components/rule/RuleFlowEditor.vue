<template>
  <div class="rule-flow-editor">
    <header class="rule-flow-editor__header">
      <span class="rule-flow-editor__title">
        <Workflow :size="16" style="margin-right:6px;vertical-align:-2px" />
        可视化规则流
      </span>
      <div class="rule-flow-editor__actions">
        <el-button :icon="Plus" size="small" @click="addConditionNode">
          添加条件
        </el-button>
        <el-button :icon="GitBranch" size="small" @click="addLogicNode">
          添加逻辑
        </el-button>
        <el-button size="small" :disabled="!canSyncFlat" @click="syncToForm">
          同步到表单
        </el-button>
      </div>
    </header>

    <div class="rule-flow-editor__canvas">
      <VueFlow
        v-model:nodes="localNodes"
        v-model:edges="localEdges"
        :default-zoom="0.8"
        :min-zoom="0.3"
        :max-zoom="2"
        fit-view-on-init
        @connect="onConnect"
        @nodes-change="onNodesChange"
        @node-drag-stop="onNodeDragStop"
      >
        <Background pattern-color="#e5e7eb" :gap="20" />
        <Controls />
        <template #node-condition="{ data }">
          <div class="condition-node" :class="{ 'is-error': !data.field, 'is-complete': data.field }">
            <Handle type="target" :position="Position.Left" />
            <div class="condition-node__header">
              <span class="condition-node__idx">C</span>
              <span class="condition-node__field">{{ data.field || '未配置字段' }}</span>
              <el-button circle size="small" type="danger" :icon="Trash2" class="condition-node__del" @click="removeNode(data.id)" />
            </div>
            <div class="condition-node__body">
              <el-select :model-value="data.field" size="small" placeholder="字段" class="cond-node-field" @change="val => updateConditionNode(data, { field: val, operator: '', value: '' })">
                <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
              <el-select :model-value="data.operator" size="small" placeholder="操作符" class="cond-node-op" @change="val => updateConditionNode(data, { operator: val })">
                <el-option v-for="op in operatorsForField(data.field)" :key="op.value" :label="op.label" :value="op.value" />
              </el-select>
              <el-input :model-value="data.value" size="small" placeholder="值" class="cond-node-val" @update:model-value="val => updateConditionNode(data, { value: val })" />
            </div>
            <Handle type="source" :position="Position.Right" />
          </div>
        </template>
        <template #node-logic="{ data }">
          <div class="logic-node">
            <Handle type="target" :position="Position.Left" />
            <div class="logic-node__header">
              <span class="logic-node__idx">L</span>
              <span class="logic-node__title">逻辑节点</span>
              <el-button v-if="data.id !== DEFAULT_ROOT_ID" circle size="small" type="danger" :icon="Trash2" class="condition-node__del" @click="removeNode(data.id)" />
            </div>
            <el-radio-group :model-value="data.logic" size="small" @change="val => updateLogicNode(data, val)">
              <el-radio-button value="AND">AND</el-radio-button>
              <el-radio-button value="OR">OR</el-radio-button>
            </el-radio-group>
            <Handle type="source" :position="Position.Right" />
          </div>
        </template>
      </VueFlow>
    </div>

    <div v-if="!validation.ok" class="rule-flow-editor__warning">
      {{ validation.errors[0] }}
    </div>
    <div v-else-if="!canSyncFlat" class="rule-flow-editor__warning">
      当前图包含嵌套逻辑，可继续编辑，但暂不能同步到扁平表单
    </div>
    <div class="rule-flow-editor__hint">
      <span>拖拽节点调整位置 | 连接线自动管理逻辑关系 | 点击"同步到表单"将流程转为条件列表</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Handle, MarkerType, Position, VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import { GitBranch, Plus, Trash2, Workflow } from 'lucide-vue-next'
import { INSURANCE_FIELDS, OPERATORS } from '../../utils/drlGenerator'
import {
  DEFAULT_ROOT_ID,
  conditionsToFlowModel,
  createConditionNode,
  createLogicNode,
  flowModelToFlatVisualModel,
  validateFlowModel
} from '../../utils/ruleFlowModel'
import { ElMessage } from 'element-plus'

const props = defineProps({
  conditions: { type: Array, default: () => [] },
  logic: { type: String, default: 'AND' },
  flowModel: { type: Object, default: null }
})

const emit = defineEmits(['update:conditions', 'update:logic', 'update:flow-model'])

const fields = INSURANCE_FIELDS
const localFlowModel = ref(props.flowModel || conditionsToFlowModel(props.conditions, props.logic))

const localNodes = ref([])
const localEdges = ref([])

const validation = computed(() => validateFlowModel(localFlowModel.value))
const canSyncFlat = computed(() => flowModelToFlatVisualModel(localFlowModel.value).ok)

watch(() => props.flowModel, (model) => {
  if (model) {
    localFlowModel.value = cloneModel(model)
    rebuildVueFlow()
  }
}, { deep: true })

watch(() => [props.conditions, props.logic], ([conditions, logic]) => {
  if (!props.flowModel) {
    localFlowModel.value = conditionsToFlowModel(conditions, logic)
    rebuildVueFlow()
  }
}, { deep: true })

rebuildVueFlow()

function rebuildVueFlow() {
  localNodes.value = localFlowModel.value.nodes.map(toVueFlowNode)
  localEdges.value = localFlowModel.value.edges.map(toVueFlowEdge)
}

function toVueFlowNode(node) {
  return {
    id: node.id,
    type: node.type === 'logic' ? 'logic' : 'condition',
    position: node.position,
    data: { ...node }
  }
}

function toVueFlowEdge(edge) {
  return {
    ...edge,
    type: 'smoothstep',
    animated: true,
    style: { stroke: '#64748b', strokeWidth: 2 },
    markerEnd: { type: MarkerType.ArrowClosed, color: '#64748b' }
  }
}

function persistGraphFromVueFlow() {
  const nodeById = new Map(localFlowModel.value.nodes.map(node => [node.id, node]))
  localFlowModel.value = {
    ...localFlowModel.value,
    nodes: localNodes.value.map(node => ({
      ...nodeById.get(node.id),
      ...node.data,
      position: { x: node.position.x, y: node.position.y }
    })),
    edges: localEdges.value.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target
    }))
  }
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function cloneModel(model) {
  return JSON.parse(JSON.stringify(model))
}

function addConditionNode() {
  const suffix = nextUnusedNodeSuffix('cond')
  const node = createConditionNode({}, suffix)
  localFlowModel.value.nodes.push(node)
  localFlowModel.value.edges.push({
    id: `edge_${DEFAULT_ROOT_ID}_${node.id}`,
    source: DEFAULT_ROOT_ID,
    target: node.id
  })
  rebuildVueFlow()
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function addLogicNode() {
  const suffix = nextUnusedNodeSuffix('logic')
  const node = createLogicNode('AND', { x: 320, y: 80 + suffix * 120 }, `logic_${suffix}`)
  localFlowModel.value.nodes.push(node)
  localFlowModel.value.edges.push({
    id: `edge_${DEFAULT_ROOT_ID}_${node.id}`,
    source: DEFAULT_ROOT_ID,
    target: node.id
  })
  rebuildVueFlow()
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function removeNode(id) {
  if (id === DEFAULT_ROOT_ID) {
    ElMessage.warning('根逻辑节点不能删除')
    return
  }
  localFlowModel.value.nodes = localFlowModel.value.nodes.filter(node => node.id !== id)
  localFlowModel.value.edges = localFlowModel.value.edges.filter(edge => edge.source !== id && edge.target !== id)
  rebuildVueFlow()
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function onConnect(params) {
  const id = `edge_${params.source}_${params.target}`
  localEdges.value = localEdges.value.filter(edge => edge.id !== id)
  localEdges.value.push({ id, source: params.source, target: params.target })
  persistGraphFromVueFlow()
  rebuildVueFlow()
}

function onNodesChange(changes) {
  const positions = new Map(
    changes
      .filter(change => change.type === 'position' && change.position)
      .map(change => [change.id, change.position])
  )
  if (!positions.size) return

  localFlowModel.value = {
    ...localFlowModel.value,
    nodes: localFlowModel.value.nodes.map(node => {
      const position = positions.get(node.id)
      return position ? { ...node, position: { x: position.x, y: position.y } } : node
    })
  }
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function onNodeDragStop({ nodes }) {
  const positions = new Map(nodes.map(node => [node.id, node.position]))
  localNodes.value = localNodes.value.map(node => {
    const position = positions.get(node.id)
    return position ? { ...node, position: { x: position.x, y: position.y } } : node
  })
  persistGraphFromVueFlow()
}

function nextUnusedNodeSuffix(prefix) {
  const nodeIds = new Set(localFlowModel.value.nodes.map(node => node.id))
  let suffix = 1
  while (nodeIds.has(`${prefix}_${suffix}`)) {
    suffix += 1
  }
  return suffix
}

function updateConditionNode(nodeData, patch) {
  Object.assign(nodeData, patch)
  updateModelNode(nodeData.id, patch)
}

function updateLogicNode(nodeData, logic) {
  Object.assign(nodeData, { logic })
  updateModelNode(nodeData.id, { logic })
}

function updateModelNode(id, patch) {
  localFlowModel.value.nodes = localFlowModel.value.nodes.map(node =>
    node.id === id ? { ...node, ...patch } : node
  )
  rebuildVueFlow()
  emit('update:flow-model', cloneModel(localFlowModel.value))
}

function operatorsForField(field) {
  const f = fields.find(item => item.value === field)
  if (!f) return OPERATORS
  return OPERATORS.filter(op => op.types.includes(f.type))
}

function syncToForm() {
  persistGraphFromVueFlow()
  const result = flowModelToFlatVisualModel(localFlowModel.value)
  if (!result.ok) {
    ElMessage.warning(result.reason)
    return
  }
  emit('update:conditions', result.visualModel.conditions)
  emit('update:logic', result.visualModel.logic)
  ElMessage.success(`已同步 ${result.visualModel.conditions.length} 个条件`)
}
</script>

<style scoped>
.rule-flow-editor {
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.rule-flow-editor__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--color-neutral-200);
  flex-wrap: wrap;
}
.rule-flow-editor__title {
  font-size: var(--fs-sm);
  font-weight: var(--fw-semibold);
  color: var(--color-neutral-800);
  display: inline-flex;
  align-items: center;
}
.rule-flow-editor__actions {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
}
.rule-flow-editor__canvas {
  height: 400px;
  background: #f8fafc;
}
.rule-flow-editor__hint {
  padding: var(--sp-2) var(--sp-4);
  font-size: var(--fs-xs);
  color: var(--color-neutral-400);
  border-top: 1px solid var(--color-neutral-100);
}

.condition-node {
  min-width: 280px;
  background: white;
  border: 2px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  font-size: 12px;
  transition: all var(--dur-2) var(--ease);
}
.condition-node:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}
.condition-node.is-error {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}
.condition-node.is-error .condition-node__header {
  background: var(--color-danger-bg);
}
.condition-node.is-complete .condition-node__header {
  background: var(--color-primary-50);
}
.condition-node__header {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-100);
  background: var(--color-neutral-50);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}
.condition-node__idx {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-500);
  color: white;
  font-size: 10px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.condition-node.is-error .condition-node__idx {
  background: var(--color-danger);
}
.condition-node__field {
  font-weight: 600;
  color: var(--color-neutral-900);
  font-size: var(--fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.condition-node.is-error .condition-node__field {
  color: var(--color-danger-fg);
}
.condition-node__del {
  flex-shrink: 0;
  width: 22px !important;
  height: 22px !important;
  padding: 0 !important;
  margin-left: auto !important;
}
.condition-node__body {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-3);
}
.cond-node-field { width: 130px; flex-shrink: 0; }
.cond-node-op { width: 100px; flex-shrink: 0; }
.cond-node-val { flex: 1; min-width: 80px; }

.logic-node {
  min-width: 180px;
  background: white;
  border: 2px solid var(--color-primary-300);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: var(--sp-3);
}
.logic-node__header {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-bottom: var(--sp-3);
}
.logic-node__idx {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-500);
  color: white;
  font-size: 10px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.logic-node__title {
  font-weight: var(--fw-semibold);
  color: var(--color-neutral-900);
}
.rule-flow-editor__warning {
  padding: var(--sp-2) var(--sp-4);
  border-top: 1px solid var(--color-warning);
  background: var(--color-warning-bg);
  color: var(--color-warning-fg);
  font-size: var(--fs-xs);
}

.rule-flow-editor :deep(.vue-flow__edge-path) {
  stroke: #94a3b8;
  stroke-width: 2;
}
.rule-flow-editor :deep(.vue-flow__edge.animated .vue-flow__edge-path) {
  stroke-dasharray: 5;
  animation: dash 0.5s linear infinite;
}
.rule-flow-editor :deep(.vue-flow__edge-textbg) {
  fill: white;
}
.rule-flow-editor :deep(.vue-flow__edge-text) {
  font-family: var(--font-mono);
  font-size: 10px;
}

@keyframes dash {
  to { stroke-dashoffset: -10; }
}
</style>
