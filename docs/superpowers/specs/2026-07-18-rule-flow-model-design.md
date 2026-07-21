# Rule Flow Model Design

## Background

The current rule editor stores visual rules as a flat `visualModel`:

- `logic`: one global `AND` or `OR`
- `conditions`: a list of field/operator/value rows

`RuleDetail.vue` already exposes a "flow" tab and `RuleFlowEditor.vue` already uses
Vue Flow. The implementation is still tied to the flat condition list, so it cannot
represent branches, grouped logic, or stable graph edits. Node positions and edges are
also treated as temporary UI state.

This design chooses the expanded option: introduce a first-class frontend flow graph
model, then convert it to the existing flat model only when the backend conversion API
requires that shape.

## Goals

- Support a real graph editor with draggable nodes and editable connections.
- Represent grouped logic with condition nodes and logic nodes.
- Keep the existing backend contract usable during the first implementation.
- Preserve the current form editor and DRL editor workflows.
- Avoid backend schema changes in this phase.

## Non-Goals

- No backend database migration for graph persistence in this phase.
- No full nested DRL generator in the backend yet.
- No visual dependency-analysis graph; this task is only for rule authoring.
- No multi-user collaborative editing.

## User Experience

The rule detail editor keeps the existing editor mode switch:

- `表单编辑`: edits the existing flat condition list.
- `流程图`: edits the new graph model.

The flow editor shows:

- condition nodes for field/operator/value checks
- logic nodes for `AND` and `OR`
- directed edges from logic nodes to condition nodes or child logic nodes
- a toolbar for adding condition nodes, adding logic nodes, fitting the view, and
  syncing back to the form model

For this phase, syncing to the form model is allowed only when the graph can be safely
flattened:

- a single root logic node
- all children are condition nodes
- no nested logic nodes
- no disconnected condition nodes

If the graph contains nested logic, the UI keeps it in the flow editor and explains that
flat form sync is unavailable for that graph. The existing DRL generator continues to use
the flat model until nested graph-to-DRL conversion is implemented.

## Data Model

Add a frontend-only model under `src/utils/ruleFlowModel.js`.

```js
{
  version: 1,
  rootId: 'logic_root',
  nodes: [
    {
      id: 'logic_root',
      type: 'logic',
      logic: 'AND',
      position: { x: 120, y: 80 }
    },
    {
      id: 'cond_1',
      type: 'condition',
      field: 'bmi',
      operator: '>',
      value: '30',
      position: { x: 420, y: 40 }
    }
  ],
  edges: [
    { id: 'edge_1', source: 'logic_root', target: 'cond_1' }
  ]
}
```

The model is intentionally small and serializable. Vue Flow-specific node data is derived
from this model rather than stored as the source of truth.

## Conversion Rules

`conditionsToFlowModel(conditions, logic)` creates:

- one root logic node
- one condition node per condition row
- one edge from root to each condition node

`flowModelToFlatVisualModel(flowModel)` returns either:

- `{ ok: true, visualModel: { logic, conditions } }`
- `{ ok: false, reason }`

It returns `ok: false` for nested logic or disconnected/invalid graphs.

This makes limitations explicit instead of silently dropping graph structure.

## Components

### `RuleFlowEditor.vue`

Responsibilities:

- render the graph via Vue Flow
- provide toolbar actions
- edit condition node fields/operators/values
- edit logic node `AND`/`OR`
- emit `update:flow-model`
- emit `update:conditions` and `update:logic` only after a successful flat sync

### `ruleFlowModel.js`

Responsibilities:

- create IDs
- convert flat conditions to graph
- convert graph to flat visual model
- validate graph shape
- normalize node positions and edge IDs

This keeps graph correctness testable without mounting Vue Flow.

### `RuleDetail.vue`

Responsibilities:

- own `flowModel` beside the existing `visualModel`
- initialize `flowModel` from `visualModel`
- pass `flowModel` into `RuleFlowEditor`
- keep existing save/generate/test behavior based on `visualModel`

In this phase, saving a nested graph is not sent to the backend. The user must flatten or
sync before generating DRL or saving the version. This avoids pretending the current
backend can execute nested visual graph rules.

## Validation

Validation should catch:

- missing root node
- disconnected nodes
- edges that reference unknown nodes
- condition nodes without a field
- nested logic when syncing to the flat model

The editor should keep invalid nodes visible and marked as invalid. It should not mutate
user work just to make the graph valid.

## Testing

Add focused unit tests for `ruleFlowModel.js`:

- flat conditions convert to a root logic graph
- simple root-to-condition graph flattens back to existing visual model
- nested logic graph refuses flat sync with a clear reason
- disconnected nodes fail validation
- unknown edge endpoints fail validation

For the component, rely on build verification in this phase because Vue Flow and
Element Plus component mounting needs extra test harness work. Component tests can be
added as part of the later frontend test coverage task.

## Rollout

1. Add `ruleFlowModel.js` and unit tests.
2. Refactor `RuleFlowEditor.vue` to use `flowModel` as source of truth.
3. Update `RuleDetail.vue` to own and pass `flowModel`.
4. Update `docs/TASK_PLAN.md` with the new P1 implementation status.
5. Verify with frontend tests/build and the affected backend tests if contracts change.

## Open Constraints

- Docker Compose full-stack validation remains blocked on this machine because Docker CLI
  is unavailable.
- Nested graph persistence and nested graph-to-DRL generation are deliberately deferred.
