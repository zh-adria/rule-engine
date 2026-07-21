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

  it('regenerates duplicate condition IDs when building a flow model', () => {
    const model = conditionsToFlowModel([
      { id: 'shared', field: 'bmi', operator: '>', value: '30' },
      { id: 'shared', field: 'age', operator: '>=', value: '18' }
    ])

    expect(model.nodes.filter(node => node.type === 'condition').map(node => node.id)).toEqual([
      'shared',
      'cond_2'
    ])
    expect(new Set(model.edges.map(edge => edge.id)).size).toBe(model.edges.length)
  })

  it('normalizes condition IDs before checking uniqueness', () => {
    const model = conditionsToFlowModel([
      { id: 1, field: 'bmi', operator: '>', value: '30' },
      { id: '1', field: 'age', operator: '>=', value: '18' }
    ])

    expect(model.nodes.filter(node => node.type === 'condition').map(node => node.id)).toEqual([
      '1',
      'cond_2'
    ])
    expect(validateFlowModel(model)).toEqual({ ok: true, errors: [] })
  })

  it('fails validation for duplicate node IDs', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi' },
        { id: 'cond_1', type: 'condition', field: 'age' }
      ],
      edges: [{ id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'cond_1' }]
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['节点 ID cond_1 重复']
    })
  })

  it('fails validation for duplicate edge IDs', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi' },
        { id: 'cond_2', type: 'condition', field: 'age' }
      ],
      edges: [
        { id: 'edge_duplicate', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_duplicate', source: DEFAULT_ROOT_ID, target: 'cond_2' }
      ]
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['连线 ID edge_duplicate 重复']
    })
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

  it('refuses to flatten reachable non-root logic behind a condition', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30' },
        { id: 'logic_2', type: 'logic', logic: 'OR' },
        { id: 'cond_2', type: 'condition', field: 'age', operator: '>=', value: '18' }
      ],
      edges: [
        { id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_2', source: 'cond_1', target: 'logic_2' },
        { id: 'edge_3', source: 'logic_2', target: 'cond_2' }
      ]
    }

    const result = flowModelToFlatVisualModel(model)

    expect(result).toEqual({
      ok: false,
      reason: '当前图包含嵌套逻辑，无法同步到扁平条件表单'
    })
  })

  it('rejects conditions nested below another condition', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30' },
        { id: 'cond_2', type: 'condition', field: 'age', operator: '>=', value: '18' }
      ],
      edges: [
        { id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_2', source: 'cond_1', target: 'cond_2' }
      ]
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['条件节点 cond_2 必须直接连接到根逻辑节点']
    })
    expect(flowModelToFlatVisualModel(model)).toEqual({
      ok: false,
      reason: '条件节点 cond_2 必须直接连接到根逻辑节点'
    })
  })

  it('refuses to flatten a reachable unsupported nested node', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30' },
        { id: 'action_1', type: 'action' }
      ],
      edges: [
        { id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_2', source: 'cond_1', target: 'action_1' }
      ]
    }

    expect(flowModelToFlatVisualModel(model)).toEqual({
      ok: false,
      reason: '扁平条件表单只支持根逻辑节点的直接条件子节点'
    })
  })

  it('refuses to flatten a graph with a non-root outgoing edge', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30' },
        { id: 'cond_2', type: 'condition', field: 'age', operator: '>=', value: '18' }
      ],
      edges: [
        { id: 'edge_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_2', source: 'cond_1', target: 'cond_2' }
      ]
    }

    expect(flowModelToFlatVisualModel(model)).toEqual({
      ok: false,
      reason: '条件节点 cond_2 必须直接连接到根逻辑节点'
    })
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

  it('fails validation for unsupported direct child node types', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'action_1', type: 'action' }
      ],
      edges: [
        { id: 'edge_action', source: DEFAULT_ROOT_ID, target: 'action_1' }
      ]
    }

    expect(validateFlowModel(model)).toEqual({
      ok: false,
      errors: ['根逻辑节点的直接子节点 action_1 类型无效']
    })
  })

  it('flattens positionless condition nodes', () => {
    const model = {
      version: 1,
      rootId: DEFAULT_ROOT_ID,
      nodes: [
        { id: DEFAULT_ROOT_ID, type: 'logic', logic: 'AND' },
        { id: 'cond_1', type: 'condition', field: 'bmi', operator: '>', value: '30' },
        { id: 'cond_2', type: 'condition', field: 'age', operator: '>=', value: '18' }
      ],
      edges: [
        { id: 'edge_condition_1', source: DEFAULT_ROOT_ID, target: 'cond_1' },
        { id: 'edge_condition_2', source: DEFAULT_ROOT_ID, target: 'cond_2' }
      ]
    }

    expect(flowModelToFlatVisualModel(model)).toEqual({
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
})
