# Rule Flow Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-class frontend flow graph model for the visual rule editor while preserving the existing flat `visualModel` backend contract.

**Architecture:** Keep `visualModel.conditions + visualModel.logic` as the execution and DRL-generation contract. Add a separate frontend-only `flowModel` that Vue Flow renders and edits. Convert between the graph model and flat model through a tested utility module.

**Tech Stack:** Vue 3, Vue Flow, Element Plus, Vitest, Vite.

## Global Constraints

- Do not add a new graph library; use existing `@vue-flow/core`, `@vue-flow/background`, and `@vue-flow/controls`.
- Do not change backend API payloads in this phase.
- Keep nested graph-to-DRL generation out of scope for this phase.
- Keep saving and DRL generation based on the existing flat `visualModel`.
- Use ASCII in new code and docs unless an existing file already uses Chinese UI text.
- Use `apply_patch` for manual edits.

---

## File Structure

- Create `frontend/rule-engine-ui/src/utils/ruleFlowModel.js`
  - Owns graph IDs, conversion, validation, and normalization.
- Create `frontend/rule-engine-ui/src/__tests__/utils/ruleFlowModel.test.js`
  - Tests graph conversion and validation without mounting Vue Flow.
- Modify `frontend/rule-engine-ui/src/components/rule/RuleFlowEditor.vue`
  - Renders `flowModel` as Vue Flow nodes and edges.
  - Emits `update:flow-model`.
  - Emits flat `update:conditions` and `update:logic` only after successful flat sync.
- Modify `frontend/rule-engine-ui/src/views/rule/RuleDetail.vue`
  - Owns `flowModel` next to `visualModel`.
  - Initializes graph state from existing flat model.
  - Blocks DRL generation and save when graph mode contains a graph that cannot flatten.
- Modify `docs/TASK_PLAN.md`
  - Record that P1-1 has moved from assessment to implementation.

---

### Task 1: Rule Flow Model Utility

**Files:**
- Create: `frontend/rule-engine-ui/src/utils/ruleFlowModel.js`
- Create: `frontend/rule-engine-ui/src/__tests__/utils/ruleFlowModel.test.js`

**Interfaces:**
- Produces:
  - `DEFAULT_ROOT_ID: string`
  - `createConditionNode(condition?: object, index?: number): FlowNode`
  - `createLogicNode(logic?: string, position?: object, id?: string): FlowNode`
  - `conditionsToFlowModel(conditions: Array, logic?: string): FlowModel`
  - `validateFlowModel(flowModel: FlowModel): { ok: boolean, errors: string[] }`
  - `flowModelToFlatVisualModel(flowModel: FlowModel): { ok: boolean, visualModel?: object, reason?: string }`

- [ ] **Step 1: Write failing tests**

Create `frontend/rule-engine-ui/src/__tests__/utils/ruleFlowModel.test.js`:

```js
import { describe, expect, it } from 'vitest'
import {
  DEFAULT_ROOT_ID,
  conditionsToFlowModel,
  flowModelToFlatVisualModel,
  validateFlowModel
} from '../../utils/ruleFlowModel'

describe('ruleFlowModel', () => {
  it('converts flat conditions to a root logic graph', () => {
    const model = conditionsToFlowModel([
      { field: 'bmi', operator: '>', value: '30' },
      { field: 'age', operator: '>=', value: '18' }
    ], 'OR')

    expect(model.version).toBe(1)
    expect(model.rootId).toBe(DEFAULT_ROOT_ID)
    expect(model.nodes).toHaveLength(3)
    expect(model.nodes[0]).toMatchObject({ id: DEFAULT_ROOT_ID, type: 'logic', logic: 'OR' })
    expect(model.edges).toEqual([
      { id: 'edge_logic_root_cond_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
      { id: 'edge_logic_root_cond_2', source: DEFAULT_ROOT_ID, target: 'cond_2' }
    ])
  })

  it('flattens a simple root-to-condition graph', () => {
    const model = conditionsToFlowModel([
      { field: 'bmi', operator: '>', value: '30' },
      { field: 'age', operator: '>=', value: '18' }
    ], 'AND')

    const result = flowModelToFlatVisualModel(model)

    expect(result).toEqual({
      ok: true,
      visualModel: {
        logic: 'AND',
        conditions: [
          { field: 'bmi', operator: '>', value: '30' },
          { field: 'age', operator: '>=', value: '18' }
        ]
      }
    })
  })

  it('refuses to flatten nested logic', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND', position: { x: 80, y: 80 } },
        { id: 'logic_2', type: 'logic', logic: 'OR', position: { x: 320, y: 80 } },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30', position: { x: 560, y: 80 } }
      ],
      edges: [
        { id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'logic_2' },
        { id: 'edge_2', source: 'logic_2', target: 'cond_1' }
      ]
    }

    const result = flowModelToFlatVisualModel(model)

    expect(result.ok).toBe(false)
    expect(result.reason).toBe('当前图包含嵌套逻辑，无法同步到扁平条件表单')
  })

  it('fails validation for disconnected nodes', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND', position: { x: 80, y: 80 } },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30', position: { x: 320, y: 80 } }
      ],
      edges: []
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['节点 cond_1 未连接到根逻辑节点']
    })
  })

  it('fails validation for unknown edge endpoints', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND', position: { x: 80, y: 80 } }
      ],
      edges: [
        { id: 'edge_missing', source: DEFAULT_ROOT_ID, target: 'cond_missing' }
      ]
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['连线 edge_missing 指向不存在的节点 cond_missing']
    })
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm test -- src/__tests__/utils/ruleFlowModel.test.js
```

Working directory: `frontend/rule-engine-ui`

Expected: FAIL because `../../utils/ruleFlowModel` does not exist.

- [ ] **Step 3: Add model utility**

Create `frontend/rule-engine-ui/src/utils/ruleFlowModel.js`:

```js
export const DEFAULT_ROOT_ID = 'logic_root'

export function createLogicNode(logic = 'AND', position = { x: 80, y: 80 }, id = DEFAULT_ROOT_ID) {
  return {
    id,
    type: 'logic',
    logic: normalizeLogic(logic),
    position: normalizePosition(position)
  }
}

export function createConditionNode(condition = {}, index = 1) {
  return {
    id: condition.id || `cond_${index}`,
    type: 'condition',
    field: condition.field || '',
    operator: condition.operator || '',
    value: condition.value ?? '',
    position: normalizePosition(condition.position || { x: 360, y: 40 + (index - 1) * 120 })
  }
}

export function conditionsToFlowModel(conditions = [], logic = 'AND') {
  const root = createLogicNode(logic)
  const conditionNodes = conditions.map((condition, index) => createConditionNode(condition, index + 1))
  return {
    version: 1,
    rootId: DEFAULT_ROOT_ID,
    nodes: [root, ...conditionNodes],
    edges: conditionNodes.map(node => ({
      id: `edge_${DEFAULT_ROOT_ID}_${node.id}`,
      source: DEFAULT_ROOT_ID,
      target: node.id
    }))
  }
}

export function validateFlowModel(flowModel) {
  const errors = []
  if (!flowModel || flowModel.version !== 1) {
    return { ok: false, errors: ['流程图版本无效'] }
  }

  const nodes = Array.isArray(flowModel.nodes) ? flowModel.nodes : []
  const edges = Array.isArray(flowModel.edges) ? flowModel.edges : []
  const nodeIds = new Set(nodes.map(node => node.id))
  const root = nodes.find(node => node.id === flowModel.rootId)

  if (!root) {
    errors.push('缺少根逻辑节点')
  } else if (root.type !== 'logic') {
    errors.push('根节点必须是逻辑节点')
  }

  for (const edge of edges) {
    if (!nodeIds.has(edge.source)) {
      errors.push(`连线 ${edge.id} 来源不存在的节点 ${edge.source}`)
    }
    if (!nodeIds.has(edge.target)) {
      errors.push(`连线 ${edge.id} 指向不存在的节点 ${edge.target}`)
    }
  }

  const reachable = collectReachableNodeIds(flowModel.rootId, edges, nodeIds)
  for (const node of nodes) {
    if (node.id !== flowModel.rootId && !reachable.has(node.id)) {
      errors.push(`节点 ${node.id} 未连接到根逻辑节点`)
    }
    if (node.type === 'condition' && !node.field) {
      errors.push(`条件节点 ${node.id} 缺少字段`)
    }
  }

  return { ok: errors.length === 0, errors }
}

export function flowModelToFlatVisualModel(flowModel) {
  const validation = validateFlowModel(flowModel)
  if (!validation.ok) {
    return { ok: false, reason: validation.errors[0] }
  }

  const root = flowModel.nodes.find(node => node.id === flowModel.rootId)
  const children = orderedChildren(flowModel.rootId, flowModel)
  if (children.some(node => node.type === 'logic')) {
    return { ok: false, reason: '当前图包含嵌套逻辑，无法同步到扁平条件表单' }
  }

  return {
    ok: true,
    visualModel: {
      logic: normalizeLogic(root.logic),
      conditions: children
        .filter(node => node.type === 'condition')
        .map(node => ({
          field: node.field,
          operator: node.operator || '==',
          value: node.value ?? ''
        }))
    }
  }
}

function orderedChildren(parentId, flowModel) {
  const nodeById = new Map(flowModel.nodes.map(node => [node.id, node]))
  return flowModel.edges
    .filter(edge => edge.source === parentId)
    .map(edge => nodeById.get(edge.target))
    .filter(Boolean)
    .sort((a, b) => a.position.y - b.position.y || a.position.x - b.position.x)
}

function collectReachableNodeIds(rootId, edges, nodeIds) {
  const reachable = new Set()
  const queue = [rootId]
  while (queue.length) {
    const current = queue.shift()
    for (const edge of edges.filter(e => e.source === current)) {
      if (!nodeIds.has(edge.target) || reachable.has(edge.target)) {
        continue
      }
      reachable.add(edge.target)
      queue.push(edge.target)
    }
  }
  return reachable
}

function normalizeLogic(logic) {
  return logic === 'OR' ? 'OR' : 'AND'
}

function normalizePosition(position) {
  return {
    x: Number(position?.x || 0),
    y: Number(position?.y || 0)
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
npm test -- src/__tests__/utils/ruleFlowModel.test.js
```

Working directory: `frontend/rule-engine-ui`

Expected: 1 test file passed, 5 tests passed.

- [ ] **Step 5: Commit**

```powershell
git add frontend/rule-engine-ui/src/utils/ruleFlowModel.js frontend/rule-engine-ui/src/__tests__/utils/ruleFlowModel.test.js
git commit -m "feat(flow): add graph model utilities"
```

---

### Task 2: Flow Editor Graph Source Of Truth

**Files:**
- Modify: `frontend/rule-engine-ui/src/components/rule/RuleFlowEditor.vue`

**Interfaces:**
- Consumes:
  - `flowModel` prop from `RuleDetail.vue`
  - `conditionsToFlowModel`, `flowModelToFlatVisualModel`, and `validateFlowModel` from `src/utils/ruleFlowModel.js`
- Produces:
  - `update:flow-model`
  - `update:conditions`
  - `update:logic`

- [ ] **Step 1: Add component test coverage through utility behavior**

No direct Vue Flow component test is added in this task. The behavior that can be tested
without a browser is already covered in Task 1. This task relies on `npm run build` to
catch Vue compile and import errors.

- [ ] **Step 2: Update props and imports**

In `RuleFlowEditor.vue`, replace the imports and props block with:

```js
import { computed, ref, watch } from 'vue'
import { VueFlow, MarkerType } from '@vue-flow/core'
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
```

- [ ] **Step 3: Replace local state with graph-derived Vue Flow state**

Use this state in the `<script setup>` body:

```js
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
```

- [ ] **Step 4: Add graph conversion helpers**

Add these functions:

```js
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
```

- [ ] **Step 5: Add graph actions**

Add these functions:

```js
function addConditionNode() {
  const conditionCount = localFlowModel.value.nodes.filter(node => node.type === 'condition').length
  const node = createConditionNode({}, conditionCount + 1)
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
  const logicCount = localFlowModel.value.nodes.filter(node => node.type === 'logic').length
  const node = createLogicNode('AND', { x: 320, y: 80 + logicCount * 120 }, `logic_${logicCount + 1}`)
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
```

- [ ] **Step 6: Add node edit functions**

Add these functions:

```js
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
```

- [ ] **Step 7: Replace template toolbar**

Update the action buttons in the component header to:

```vue
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
```

- [ ] **Step 8: Add separate node templates**

Use separate slots:

```vue
<template #node-condition="{ data }">
  <div class="condition-node" :class="{ 'is-error': !data.field, 'is-complete': data.field }">
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
  </div>
</template>

<template #node-logic="{ data }">
  <div class="logic-node">
    <div class="logic-node__header">
      <span class="logic-node__idx">L</span>
      <span class="logic-node__title">逻辑节点</span>
      <el-button v-if="data.id !== DEFAULT_ROOT_ID" circle size="small" type="danger" :icon="Trash2" class="condition-node__del" @click="removeNode(data.id)" />
    </div>
    <el-radio-group :model-value="data.logic" size="small" @change="val => updateLogicNode(data, val)">
      <el-radio-button value="AND">AND</el-radio-button>
      <el-radio-button value="OR">OR</el-radio-button>
    </el-radio-group>
  </div>
</template>
```

- [ ] **Step 9: Add sync and status handling**

Replace `syncToForm()` with:

```js
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
```

Add this status message near the hint area:

```vue
<div v-if="!validation.ok" class="rule-flow-editor__warning">
  {{ validation.errors[0] }}
</div>
<div v-else-if="!canSyncFlat" class="rule-flow-editor__warning">
  当前图包含嵌套逻辑，可继续编辑，但暂不能同步到扁平表单
</div>
```

- [ ] **Step 10: Update styles**

Add:

```css
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
```

- [ ] **Step 11: Build verification**

Run:

```powershell
npm run build
```

Working directory: `frontend/rule-engine-ui`

Expected: Vite build succeeds. Existing chunk size warnings are acceptable.

- [ ] **Step 12: Commit**

```powershell
git add frontend/rule-engine-ui/src/components/rule/RuleFlowEditor.vue
git commit -m "feat(flow): edit graph rules in Vue Flow"
```

---

### Task 3: Rule Detail Flow Model Integration

**Files:**
- Modify: `frontend/rule-engine-ui/src/views/rule/RuleDetail.vue`

**Interfaces:**
- Consumes:
  - `conditionsToFlowModel` from `src/utils/ruleFlowModel.js`
  - `flowModelToFlatVisualModel` from `src/utils/ruleFlowModel.js`
- Produces:
  - `flowModel` state passed into `RuleFlowEditor`

- [ ] **Step 1: Import model helpers**

Add to `RuleDetail.vue` imports:

```js
import {
  conditionsToFlowModel,
  flowModelToFlatVisualModel
} from '../../utils/ruleFlowModel'
```

- [ ] **Step 2: Add flow state**

Near `const visualModel = reactive({ logic: 'AND', conditions: [] })`, add:

```js
const flowModel = ref(conditionsToFlowModel(visualModel.conditions, visualModel.logic))
```

- [ ] **Step 3: Pass flow model into editor**

Replace the `RuleFlowEditor` usage with:

```vue
<RuleFlowEditor
  :conditions="visualModel.conditions"
  :logic="visualModel.logic"
  :flow-model="flowModel"
  @update:flow-model="flowModel = $event"
  @update:conditions="visualModel.conditions = $event"
  @update:logic="visualModel.logic = $event"
/>
```

- [ ] **Step 4: Keep form changes reflected in graph only while in form mode**

Add this watcher:

```js
watch(
  () => [visualModel.logic, JSON.stringify(visualModel.conditions), editorMode.value],
  () => {
    if (editorMode.value === 'form') {
      flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
    }
  }
)
```

- [ ] **Step 5: Guard DRL generation**

At the start of `generateDrlFromVisual()`, after the empty-condition check, add:

```js
if (editorMode.value === 'flow') {
  const flat = flowModelToFlatVisualModel(flowModel.value)
  if (!flat.ok) {
    ElMessage.warning(flat.reason)
    return
  }
  visualModel.conditions = flat.visualModel.conditions
  visualModel.logic = flat.visualModel.logic
}
```

- [ ] **Step 6: Reset graph after parsing DRL**

In `parseDrlToVisual()`, after each successful assignment to `visualModel.conditions`,
add:

```js
flowModel.value = conditionsToFlowModel(visualModel.conditions, visualModel.logic)
```

There are two successful parse branches:

- backend parse branch after `visualModel.conditions = result.visualModel.conditions || []`
- local parse branch after `visualModel.conditions = conditions`

- [ ] **Step 7: Include graph in dirty-state snapshot**

In `serializeState()`, add `flowModel`:

```js
flowModel: JSON.parse(JSON.stringify(flowModel.value))
```

- [ ] **Step 8: Build verification**

Run:

```powershell
npm run build
```

Working directory: `frontend/rule-engine-ui`

Expected: Vite build succeeds. Existing chunk size warnings are acceptable.

- [ ] **Step 9: Commit**

```powershell
git add frontend/rule-engine-ui/src/views/rule/RuleDetail.vue
git commit -m "feat(flow): wire graph model into rule detail"
```

---

### Task 4: Documentation And Full Verification

**Files:**
- Modify: `docs/TASK_PLAN.md`

**Interfaces:**
- Consumes:
  - Implementation status from Tasks 1 through 3.
- Produces:
  - Updated roadmap status for P1-1.

- [ ] **Step 1: Update P1-1 status**

In `docs/TASK_PLAN.md`, update the `拖拽式可视化规则编辑器` section to record:

```markdown
- **状态**：🚧 已进入实现
- **当前设计**：采用独立 frontend `flowModel`，通过转换工具与既有 `visualModel.conditions + logic` 兼容。
- **本阶段边界**：支持图编辑和扁平同步；嵌套图可编辑但暂不生成 DRL。
```

- [ ] **Step 2: Record verification**

Add a verification block dated `2026-07-18`:

```markdown
### 2026-07-18 P1-1 验证记录

| 命令 | 结果 |
|------|------|
| `cd frontend/rule-engine-ui && npm test` | ✅ 通过 |
| `cd frontend/rule-engine-ui && npm run build` | ✅ 通过；仍有既有 chunk size warning |
```

- [ ] **Step 3: Run frontend tests**

Run:

```powershell
npm test
```

Working directory: `frontend/rule-engine-ui`

Expected: all frontend tests pass.

- [ ] **Step 4: Run frontend build**

Run:

```powershell
npm run build
```

Working directory: `frontend/rule-engine-ui`

Expected: Vite build succeeds. Existing chunk size warnings are acceptable.

- [ ] **Step 5: Check git status**

Run:

```powershell
git status --short
```

Expected: only the intended documentation file is modified.

- [ ] **Step 6: Commit**

```powershell
git add docs/TASK_PLAN.md
git commit -m "docs(flow): update editor task status"
```

---

## Final Verification

After all tasks are committed, run:

```powershell
npm test
```

Working directory: `frontend/rule-engine-ui`

Expected: all frontend tests pass.

Then run:

```powershell
npm run build
```

Working directory: `frontend/rule-engine-ui`

Expected: Vite build succeeds. Existing chunk size warnings are acceptable.

Then run:

```powershell
git status --short
```

Expected: no output.

## Spec Coverage Review

- First-class graph model: Task 1.
- Draggable Vue Flow graph editor: Task 2.
- Existing backend contract preserved: Tasks 1 and 3.
- Form and DRL workflows preserved: Task 3.
- Documentation updated: Task 4.
- Docker Compose blocker recorded in the design spec; this implementation plan does not require Docker.
