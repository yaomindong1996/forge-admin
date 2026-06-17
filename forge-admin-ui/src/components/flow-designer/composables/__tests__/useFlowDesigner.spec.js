import { describe, expect, it } from 'vitest'
import { createEmptyFlow, useFlowDesigner } from '../useFlowDesigner.js'

describe('createEmptyFlow', () => {
  it('返回 start → end 默认两节点 + 一条边', () => {
    const json = createEmptyFlow()
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    expect(json.edges).toHaveLength(1)
    expect(json.edges[0]).toMatchObject({
      source: 'StartEvent_1',
      target: 'EndEvent_1',
      isDefault: false,
    })
  })
})

describe('useFlowDesigner - 查询', () => {
  it('getNode / getEdge / findStartNode / findEndNode', () => {
    const { getNode, getEdge, findStartNode, findEndNode } = useFlowDesigner()
    expect(getNode('StartEvent_1').nodeType).toBe('start')
    expect(getEdge('Flow_1').source).toBe('StartEvent_1')
    expect(findStartNode().id).toBe('StartEvent_1')
    expect(findEndNode().id).toBe('EndEvent_1')
  })

  it('getOutgoingEdges / getIncomingEdges', () => {
    const { getOutgoingEdges, getIncomingEdges } = useFlowDesigner()
    expect(getOutgoingEdges('StartEvent_1')).toHaveLength(1)
    expect(getIncomingEdges('EndEvent_1')).toHaveLength(1)
  })
})

describe('useFlowDesigner - addNode', () => {
  it('在 start 后插入审批节点：原边被替换为两条新边', () => {
    const designer = useFlowDesigner()
    const newId = designer.addNode('StartEvent_1', 'approver')
    const json = designer.flowJson.value
    expect(json.nodes).toHaveLength(3)
    const direct = json.edges.find(e => e.source === 'StartEvent_1' && e.target === 'EndEvent_1')
    expect(direct).toBeUndefined()
    expect(json.edges.find(e => e.source === 'StartEvent_1' && e.target === newId)).toBeTruthy()
    expect(json.edges.find(e => e.source === newId && e.target === 'EndEvent_1')).toBeTruthy()
  })

  it('afterId 不存在抛错', () => {
    const designer = useFlowDesigner()
    expect(() => designer.addNode('NotExist', 'approver')).toThrow(/不存在/)
  })

  it('插入新节点继承原边 condition / branchId', () => {
    const designer = useFlowDesigner()
    designer.updateEdge('Flow_1', { condition: 'test', branchId: 'b1' })
    const newId = designer.addNode('StartEvent_1', 'approver')
    const json = designer.flowJson.value
    const upstream = json.edges.find(e => e.target === newId)
    const downstream = json.edges.find(e => e.source === newId)
    expect(upstream.condition).toBe('test')
    expect(upstream.branchId).toBe('b1')
    expect(downstream.condition).toBe('')
    expect(downstream.branchId).toBe(null)
  })
})

describe('useFlowDesigner - deleteNode 普通节点', () => {
  it('中间节点删除后入边接到出边 target', () => {
    const designer = useFlowDesigner()
    const id1 = designer.addNode('StartEvent_1', 'approver')
    const id2 = designer.addNode(id1, 'approver')
    designer.deleteNode(id1)
    const json = designer.flowJson.value
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end', 'approver'])
    expect(json.edges.find(e => e.source === 'StartEvent_1' && e.target === id2)).toBeTruthy()
  })

  it('start / end 节点删除抛错', () => {
    const designer = useFlowDesigner()
    expect(() => designer.deleteNode('StartEvent_1')).toThrow(/start/)
    expect(() => designer.deleteNode('EndEvent_1')).toThrow(/end/)
  })

  it('未知 nodeId 抛错', () => {
    const designer = useFlowDesigner()
    expect(() => designer.deleteNode('NoSuch')).toThrow(/不存在/)
  })

  it('删除选中节点时清除 selectedNodeId', () => {
    const designer = useFlowDesigner()
    const id = designer.addNode('StartEvent_1', 'approver')
    designer.selectedNodeId.value = id
    designer.deleteNode(id)
    expect(designer.selectedNodeId.value).toBe(null)
  })
})

describe('useFlowDesigner - updateNode', () => {
  it('顶层字段直接覆盖', () => {
    const designer = useFlowDesigner()
    designer.updateNode('StartEvent_1', { name: '我的发起' })
    expect(designer.getNode('StartEvent_1').name).toBe('我的发起')
  })

  it('config 字段浅合并，不影响其他字段', () => {
    const designer = useFlowDesigner()
    const id = designer.addNode('StartEvent_1', 'approver')
    designer.updateNode(id, { config: { assignee: 'spel', assigneeExpr: 'x' } })
    const node = designer.getNode(id)
    expect(node.config.assignee).toBe('spel')
    expect(node.config.assigneeExpr).toBe('x')
    expect(node.config.allowApprove).toBe(true)
    expect(node.config.taskType).toBe('assignee')
  })

  it('未知 nodeId 抛错', () => {
    const designer = useFlowDesigner()
    expect(() => designer.updateNode('NoSuch', { name: 'x' })).toThrow(/不存在/)
  })
})

describe('useFlowDesigner - moveNodeUp / Down', () => {
  it('线性链中间向下 / 向上交换', () => {
    const designer = useFlowDesigner()
    const a = designer.addNode('StartEvent_1', 'approver')
    const b = designer.addNode(a, 'approver')
    designer.moveNodeDown(a)
    let json = designer.flowJson.value
    expect(json.edges.find(e => e.source === 'StartEvent_1' && e.target === b)).toBeTruthy()
    expect(json.edges.find(e => e.source === b && e.target === a)).toBeTruthy()
    expect(json.edges.find(e => e.source === a && e.target === 'EndEvent_1')).toBeTruthy()

    designer.moveNodeUp(a)
    json = designer.flowJson.value
    expect(json.edges.find(e => e.source === 'StartEvent_1' && e.target === a)).toBeTruthy()
    expect(json.edges.find(e => e.source === a && e.target === b)).toBeTruthy()
    expect(json.edges.find(e => e.source === b && e.target === 'EndEvent_1')).toBeTruthy()
  })

  it('已经在最顶 / 最底端抛错', () => {
    const designer = useFlowDesigner()
    const a = designer.addNode('StartEvent_1', 'approver')
    expect(() => designer.moveNodeUp(a)).toThrow(/最顶端/)
    expect(() => designer.moveNodeDown(a)).toThrow(/最底端/)
  })
})

describe('useFlowDesigner - copyNode', () => {
  it('复制节点：新节点位于原节点之后，name 含 "副本"', () => {
    const designer = useFlowDesigner()
    const a = designer.addNode('StartEvent_1', 'approver')
    designer.updateNode(a, { name: '部门审批' })
    const cloneId = designer.copyNode(a)
    const cloned = designer.getNode(cloneId)
    expect(cloned.name).toBe('部门审批 副本')
    expect(cloned.id).not.toBe(a)
    // 链路：Start → a → cloneId → End
    const json = designer.flowJson.value
    expect(json.edges.find(e => e.source === a && e.target === cloneId)).toBeTruthy()
  })

  it('start / end 复制抛错', () => {
    const designer = useFlowDesigner()
    expect(() => designer.copyNode('StartEvent_1')).toThrow()
    expect(() => designer.copyNode('EndEvent_1')).toThrow()
  })
})

describe('useFlowDesigner - loadJson / exportJson / reset', () => {
  it('loadJson 替换内部状态并重置 selectedNodeId', () => {
    const designer = useFlowDesigner()
    designer.selectedNodeId.value = 'X'
    designer.loadJson({
      processId: 'P',
      processName: 'N',
      nodes: [{ id: 'A', nodeType: 'start', name: '', config: {} }],
      edges: [],
    })
    expect(designer.flowJson.value.processId).toBe('P')
    expect(designer.selectedNodeId.value).toBe(null)
  })

  it('exportJson 返回深拷贝（修改返回值不影响内部）', () => {
    const designer = useFlowDesigner()
    const exp = designer.exportJson()
    exp.nodes.push({ id: 'X', nodeType: 'approver', config: {} })
    expect(designer.flowJson.value.nodes).toHaveLength(2)
  })

  it('reset 回到 createEmptyFlow', () => {
    const designer = useFlowDesigner()
    designer.addNode('StartEvent_1', 'approver')
    designer.reset()
    expect(designer.flowJson.value.nodes).toHaveLength(2)
    expect(designer.flowJson.value.processName).toBe('新流程')
  })
})
