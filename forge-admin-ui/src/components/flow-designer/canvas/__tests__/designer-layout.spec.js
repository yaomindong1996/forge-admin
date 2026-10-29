import { describe, expect, it } from 'vitest'
import { createEmptyFlow, useFlowDesigner } from '../../composables/useFlowDesigner.js'
import { layoutDesignerFlow } from '../designer-layout.js'

describe('审批画布展示布局', () => {
  it('只为同层独立后继提供条件卡，不修改原流程及边顺序', () => {
    const designer = useFlowDesigner(createEmptyFlow())
    const gateway = designer.addNode('StartEvent_1', 'condition')
    const before = JSON.stringify(designer.flowJson.value)
    const layout = layoutDesignerFlow(designer.flowJson.value)
    const outgoing = designer.getOutgoingEdges(gateway)
    expect(layout.branchCards.size).toBe(2)
    for (const edge of outgoing) {
      const card = layout.branchCards.get(edge.id)
      const target = layout.nodePositions.get(edge.target)
      const path = layout.edgePaths.get(edge.id).points
      expect(card.x).toBe(target.x + target.width / 2)
      expect(card.y + 32).toBeLessThan(target.y)
      expect(path[1].y).toBeLessThan(card.y - 32)
    }
    expect(JSON.stringify(designer.flowJson.value)).toBe(before)
  })

  it('多个出口共享后继时不叠放条件卡', () => {
    const flow = createEmptyFlow()
    flow.nodes.splice(1, 0, { id: 'GW', nodeType: 'condition', config: {} })
    flow.edges = [
      { id: 'F1', source: 'StartEvent_1', target: 'GW' },
      { id: 'F2', source: 'GW', target: 'EndEvent_1' },
      { id: 'F3', source: 'GW', target: 'EndEvent_1', isDefault: true },
    ]
    expect(layoutDesignerFlow(flow).branchCards.size).toBe(0)
  })

  it('自由拖拽冻结坐标后仍保留并行/条件分支卡，不退回空标签', () => {
    const designer = useFlowDesigner(createEmptyFlow())
    const gateway = designer.addNode('StartEvent_1', 'parallel')
    const layoutBefore = layoutDesignerFlow(designer.flowJson.value)
    expect(layoutBefore.branchCards.size).toBe(2)

    // 模拟拖拽其它节点后冻结全部坐标
    for (const node of designer.flowJson.value.nodes) {
      const pos = layoutBefore.nodePositions.get(node.id)
      if (pos)
        node.uiPosition = { x: pos.x, y: pos.y }
    }
    designer.flowJson.value = { ...designer.flowJson.value }

    const layoutAfter = layoutDesignerFlow(designer.flowJson.value)
    expect(layoutAfter.branchCards.size).toBe(2)
    for (const edge of designer.getOutgoingEdges(gateway)) {
      expect(layoutAfter.branchCards.get(edge.id)).toBeTruthy()
      expect(layoutAfter.edgePaths.get(edge.id).points.length).toBeGreaterThanOrEqual(3)
    }
  })
})
