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
})
