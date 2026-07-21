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
    id: normalizeNodeId(condition.id, `cond_${index}`),
    type: 'condition',
    field: condition.field || '',
    operator: condition.operator || '',
    value: condition.value ?? '',
    position: normalizePosition(condition.position || { x: 360, y: 40 + (index - 1) * 120 })
  }
}

export function conditionsToFlowModel(conditions = [], logic = 'AND') {
  const root = createLogicNode(logic)
  const usedConditionIds = new Set([DEFAULT_ROOT_ID])
  const conditionNodes = conditions.map((condition, index) => {
    let id = normalizeNodeId(condition.id, `cond_${index + 1}`)
    let suffix = index
    while (usedConditionIds.has(id)) {
      id = `cond_${++suffix}`
    }
    usedConditionIds.add(id)
    return createConditionNode({ ...condition, id }, index + 1)
  })
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

  for (const duplicateId of duplicateIds(nodes.map(node => node.id))) {
    errors.push(`节点 ID ${duplicateId} 重复`)
  }
  for (const duplicateId of duplicateIds(edges.map(edge => edge.id))) {
    errors.push(`连线 ID ${duplicateId} 重复`)
  }

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

  if (root?.type === 'logic') {
    for (const edge of edges.filter(edge => edge.source === flowModel.rootId)) {
      const child = nodes.find(node => node.id === edge.target)
      if (child && child.type !== 'condition' && child.type !== 'logic') {
        errors.push(`根逻辑节点的直接子节点 ${child.id} 类型无效`)
      }
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
    if (node.type === 'condition' && root?.type === 'logic' && reachable.has(node.id)) {
      const hasConditionParent = edges.some(edge => {
        const parent = nodes.find(candidate => candidate.id === edge.source)
        return edge.target === node.id && parent?.type === 'condition'
      })
      if (hasConditionParent) {
        errors.push(`条件节点 ${node.id} 必须直接连接到根逻辑节点`)
      }
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
  const reachable = collectReachableNodeIds(flowModel.rootId, flowModel.edges, new Set(flowModel.nodes.map(node => node.id)))
  if (flowModel.nodes.some(node => node.id !== flowModel.rootId && node.type === 'logic' && reachable.has(node.id))) {
    return { ok: false, reason: '当前图包含嵌套逻辑，无法同步到扁平条件表单' }
  }
  if (flowModel.nodes.some(node => node.id !== flowModel.rootId && reachable.has(node.id) && node.type !== 'condition')) {
    return { ok: false, reason: '扁平条件表单只支持根逻辑节点的直接条件子节点' }
  }
  if (flowModel.edges.some(edge => edge.source !== flowModel.rootId)) {
    return { ok: false, reason: '扁平条件表单不支持非根节点的出边' }
  }

  const children = orderedChildren(flowModel.rootId, flowModel)

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
    .sort((a, b) => (a.position?.y ?? 0) - (b.position?.y ?? 0) || (a.position?.x ?? 0) - (b.position?.x ?? 0))
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

function duplicateIds(ids) {
  const seen = new Set()
  const duplicates = new Set()
  for (const id of ids) {
    if (seen.has(id)) {
      duplicates.add(id)
    }
    seen.add(id)
  }
  return duplicates
}

function normalizeLogic(logic) {
  return logic === 'OR' ? 'OR' : 'AND'
}

function normalizeNodeId(id, fallback) {
  const normalizedId = id == null ? '' : String(id).trim()
  return normalizedId || fallback
}

function normalizePosition(position) {
  return {
    x: Number(position?.x || 0),
    y: Number(position?.y || 0)
  }
}
