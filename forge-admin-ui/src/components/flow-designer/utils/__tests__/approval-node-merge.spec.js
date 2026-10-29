import { describe, expect, it } from 'vitest'
import { useFlowDesigner } from '../../composables/useFlowDesigner.js'
import { useFlowHistory } from '../../composables/useFlowHistory.js'
import { convertBpmnToJson } from '../../converter/bpmn-to-json.js'
import { convertJsonToBpmn } from '../../converter/json-to-bpmn.js'
import { getApprovalMergeCandidates, inspectApprovalMerge, mergeApprovalNodes } from '../approval-node-merge.js'
import { approvalMergeFixture } from './approval-merge-fixture.js'

const node = (flow, id) => flow.nodes.find(item => item.id === id)
const edge = (flow, id) => flow.edges.find(item => item.id === id)

describe('条件支路公共审批合并', () => {
  it('原图不变，只删除来源节点，保留目标全部配置和各入边元数据', () => {
    const original = approvalMergeFixture()
    const before = JSON.stringify(original)
    const kept = JSON.parse(JSON.stringify(node(original, 'A2')))
    const next = mergeApprovalNodes(original, 'A1', 'A2')
    expect(JSON.stringify(original)).toBe(before)
    expect(next.nodes).toHaveLength(original.nodes.length - 1)
    expect(node(next, 'A1')).toBeUndefined()
    expect(node(next, 'A2')).toEqual({ ...kept, config: { ...kept.config, mergeNode: true } })
    expect(edge(next, 'HA1')).toEqual({ ...edge(original, 'HA1'), target: 'A2' })
    expect(edge(next, 'GH1')).toEqual(edge(original, 'GH1'))
    expect(edge(next, 'GH2')).toEqual(edge(original, 'GH2'))
    expect(edge(next, 'AE2')).toEqual(edge(original, 'AE2'))
    expect(edge(next, 'AE1')).toBeUndefined()
  })

  it('相同配置对象键顺序无关，mergeNode 布局标记不阻止合并', () => {
    const flow = approvalMergeFixture()
    node(flow, 'A2').config = Object.fromEntries(Object.entries(node(flow, 'A2').config).reverse())
    node(flow, 'A2').config.mergeNode = true
    expect(inspectApprovalMerge(flow, 'A1', 'A2').eligible).toBe(true)
  })

  it.each([
    ['candidateUsers', ['another-user']],
    ['requireComment', false],
    ['requireSignature', true],
    ['multiInstanceType', 'parallel'],
    ['formKey', 'another-form'],
    ['formFieldPermissions', [{ field: 'amount', editable: false }]],
    ['formChildPermissions', [{ childKey: 'items', allowDelete: true }]],
    ['formArrayPermissions', [{ arrayKey: 'items', readable: false }]],
    ['taskListeners', [{ event: 'complete', type: 'class', value: 'fixture.Listener' }]],
    ['executionListeners', [{ event: 'end', type: 'class', value: 'fixture.Listener' }]],
    ['approvalPoints', [{ id: 'point', content: '核查金额', required: true }]],
    ['printTemplatePolicy', 'RESTRICT'],
    ['unknownExtension', { permission: 'restricted' }],
  ])('审批人看似相同但 %s 不一致时拒绝', (key, value) => {
    const flow = approvalMergeFixture()
    node(flow, 'A2').config[key] = value
    const before = JSON.stringify(flow)
    expect(inspectApprovalMerge(flow, 'A1', 'A2').differenceKeys).toContain(key)
    expect(() => mergeApprovalNodes(flow, 'A1', 'A2')).toThrow('审批配置不一致')
    expect(JSON.stringify(flow)).toBe(before)
  })

  it.each(['parallel', 'inclusive'])('%s 不能通过直接合并隐式改变并发语义', (type) => {
    expect(() => mergeApprovalNodes(approvalMergeFixture(type), 'A1', 'A2')).toThrow('仅支持互斥条件分支')
  })

  it('不同后继、不同出边条件、跨网关、回流、同一节点均拒绝', () => {
    const differentNext = approvalMergeFixture()
    edge(differentNext, 'AE1').target = 'H2'
    expect(inspectApprovalMerge(differentNext, 'A1', 'A2').eligible).toBe(false)
    const conditioned = approvalMergeFixture()
    edge(conditioned, 'AE1').condition = '$' + '{approved}'
    expect(() => mergeApprovalNodes(conditioned, 'A1', 'A2')).toThrow('后续连线条件')
    const nested = approvalMergeFixture('condition', 3)
    node(nested, 'H2').nodeType = 'condition'
    nested.edges.push({ id: 'H2A3', source: 'H2', target: 'A3' })
    expect(() => mergeApprovalNodes(nested, 'A1', 'A2')).toThrow('同一个条件网关')
    const cycle = approvalMergeFixture()
    cycle.edges.push({ id: 'ES', source: 'E', target: 'S' })
    expect(() => mergeApprovalNodes(cycle, 'A1', 'A2')).toThrow('回流')
    expect(() => mergeApprovalNodes(cycle, 'A1', 'A1')).toThrow('自身')
  })

  it('普通任务分叉不是互斥分支，不能合并', () => {
    const flow = approvalMergeFixture()
    node(flow, 'G').nodeType = 'approver'
    expect(() => mergeApprovalNodes(flow, 'A1', 'A2')).toThrow('仅支持互斥条件分支')
  })

  it('同支路串联节点、存在多出边的审批和无效后继均拒绝', () => {
    const flow = approvalMergeFixture()
    expect(() => mergeApprovalNodes(flow, 'H1', 'A1')).toThrow('不同支路')
    const fork = approvalMergeFixture()
    fork.edges.push({ id: 'extra', source: 'A1', target: 'H2' })
    expect(() => mergeApprovalNodes(fork, 'A1', 'A2')).toThrow('只有一个后续节点')
    const invalid = approvalMergeFixture()
    edge(invalid, 'AE1').target = 'missing'
    expect(() => mergeApprovalNodes(invalid, 'A1', 'A2')).toThrow('后续节点不存在')
  })

  it('目标已引用来源所在支路时不能再合并一次', () => {
    const flow = approvalMergeFixture()
    flow.edges.push({ id: 'HA12', source: 'H1', target: 'A2' })
    expect(inspectApprovalMerge(flow, 'A1', 'A2').eligible).toBe(false)
  })

  it('节点级未知扩展不同不允许覆盖，只有出边的布局 branchId 差异可以忽略', () => {
    const flow = approvalMergeFixture()
    node(flow, 'A2').customRuntimeConfig = { callback: 'by-node-key' }
    expect(() => mergeApprovalNodes(flow, 'A1', 'A2')).toThrow('节点级扩展')
    delete node(flow, 'A2').customRuntimeConfig
    edge(flow, 'AE1').branchId = 'left-layout'
    edge(flow, 'AE2').branchId = 'right-layout'
    expect(inspectApprovalMerge(flow, 'A1', 'A2').eligible).toBe(true)
  })

  it('已有公共节点可继续合并第三支路，后续新增分支连接公共节点', () => {
    const first = mergeApprovalNodes(approvalMergeFixture('condition', 3), 'A1', 'A2')
    const next = mergeApprovalNodes(first, 'A3', 'A2')
    expect(next.edges.filter(item => item.target === 'A2')).toHaveLength(3)
    const designer = useFlowDesigner(next)
    const added = designer.addBranch('G')
    expect(designer.getOutgoingEdges(added.nodeId)[0].target).toBe('A2')
  })

  it('合并一次只记一条历史，撤销和重做恢复完整图；失败不记空历史', () => {
    const flow = approvalMergeFixture()
    const designer = useFlowDesigner(flow)
    const history = useFlowHistory(designer.flowJson)
    designer.mergeApprovalNodes('A1', 'A2', history.snapshot)
    expect(history.undoStack.value).toHaveLength(1)
    expect(history.undo()).toBe(true)
    expect(designer.exportJson()).toEqual(flow)
    expect(history.redo()).toBe(true)
    expect(designer.getIncomingEdges('A2')).toHaveLength(2)
    expect(() => designer.mergeApprovalNodes('missing', 'A2', history.snapshot)).toThrow()
    expect(history.undoStack.value).toHaveLength(1)
  })

  it('xML 重新导入仍只有一个目标审批，条件和默认分支不丢', () => {
    // 同时覆盖真实 XML 导入后的节点扩展/默认配置，不能只让手工 JSON 可以合并。
    const imported = convertBpmnToJson(convertJsonToBpmn(approvalMergeFixture()))
    const merged = mergeApprovalNodes(imported, 'A1', 'A2')
    const xml = convertJsonToBpmn(merged)
    expect(xml).not.toContain('<bpmn:userTask id="A1"')
    expect(xml.match(/<bpmn:userTask id="A2"/g)).toHaveLength(1)
    const restored = convertBpmnToJson(xml)
    expect(restored.edges.filter(item => item.target === 'A2')).toHaveLength(2)
    expect(node(restored, 'A2').config.candidateUsers).toEqual(['fixture-admin'])
    expect(edge(restored, 'GH1').condition).toBe(edge(merged, 'GH1').condition)
    expect(edge(restored, 'GH2').isDefault).toBe(true)
    expect(node(restored, 'G').config.defaultFlowId).toBe('GH2')
    const designer = useFlowDesigner(restored)
    designer.deleteNode('G')
    // 网关软删：分支头节点保留，公共审批不得被级联删除
    expect(designer.getNode('A2')).not.toBeNull()
    expect(designer.getOutgoingEdges('S').length).toBeGreaterThan(0)
    expect(designer.getNode('G')).toBeNull()
  })

  it('分支入口本身合并时保留两条互斥边；删除网关不删除公共审批', () => {
    const flow = approvalMergeFixture()
    flow.nodes = flow.nodes.filter(item => !['H1', 'H2'].includes(item.id))
    flow.edges = flow.edges.filter(item => !['HA1', 'HA2'].includes(item.id))
    edge(flow, 'GH1').target = 'A1'
    edge(flow, 'GH2').target = 'A2'
    const merged = mergeApprovalNodes(flow, 'A1', 'A2')
    expect(merged.edges.filter(item => item.source === 'G' && item.target === 'A2')).toHaveLength(2)
    const designer = useFlowDesigner(merged)
    designer.deleteNode('G')
    expect(designer.getNode('A2')).not.toBeNull()
    expect(designer.getOutgoingEdges('S').every(e => e.target === 'A2')).toBe(true)
  })

  it('候选列表取真实节点并给出不可合并原因', () => {
    const candidates = getApprovalMergeCandidates(approvalMergeFixture(), 'A1')
    expect(candidates.find(item => item.node.id === 'A2').eligible).toBe(true)
    expect(candidates.find(item => item.node.id === 'H1').eligible).toBe(false)
    expect(candidates.every(item => item.node.id !== 'A1')).toBe(true)
  })
})
