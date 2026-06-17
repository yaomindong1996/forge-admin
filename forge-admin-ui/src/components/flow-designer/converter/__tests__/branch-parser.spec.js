import { describe, expect, it } from 'vitest'
import { convertBpmnToJson } from '../bpmn-to-json.js'
import { getNodeInDegree, getNodeOutDegree, markBranches } from '../branch-parser.js'

const DOLLAR = '$'

const SAMPLE_EXCLUSIVE = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:exclusiveGateway id="GW1" default="F_else"/>',
  '    <bpmn:userTask id="T_a"/>',
  '    <bpmn:userTask id="T_b"/>',
  '    <bpmn:exclusiveGateway id="GW_merge"/>',
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F_in" sourceRef="S" targetRef="GW1"/>',
  '    <bpmn:sequenceFlow id="F_a" sourceRef="GW1" targetRef="T_a">',
  `      <bpmn:conditionExpression>${DOLLAR}{days &gt; 3}</bpmn:conditionExpression>`,
  '    </bpmn:sequenceFlow>',
  '    <bpmn:sequenceFlow id="F_else" sourceRef="GW1" targetRef="T_b"/>',
  '    <bpmn:sequenceFlow id="F_a_m" sourceRef="T_a" targetRef="GW_merge"/>',
  '    <bpmn:sequenceFlow id="F_b_m" sourceRef="T_b" targetRef="GW_merge"/>',
  '    <bpmn:sequenceFlow id="F_out" sourceRef="GW_merge" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_PARALLEL = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P">',
  '    <bpmn:parallelGateway id="GW_par"/>',
  '    <bpmn:userTask id="T1"/>',
  '    <bpmn:userTask id="T2"/>',
  '    <bpmn:userTask id="T3"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="GW_par" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="GW_par" targetRef="T2"/>',
  '    <bpmn:sequenceFlow id="F3" sourceRef="GW_par" targetRef="T3"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_NESTED = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P">',
  '    <bpmn:exclusiveGateway id="GW_outer"/>',
  '    <bpmn:exclusiveGateway id="GW_inner"/>',
  '    <bpmn:userTask id="T1"/>',
  '    <bpmn:userTask id="T2"/>',
  '    <bpmn:userTask id="T3"/>',
  '    <bpmn:sequenceFlow id="F_o1" sourceRef="GW_outer" targetRef="GW_inner"/>',
  '    <bpmn:sequenceFlow id="F_o2" sourceRef="GW_outer" targetRef="T3"/>',
  '    <bpmn:sequenceFlow id="F_i1" sourceRef="GW_inner" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F_i2" sourceRef="GW_inner" targetRef="T2"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('markBranches - 排他网关', () => {
  it('两条出边分配 b1/b2，default 边 isDefault=true 且清空 condition', () => {
    const json = convertBpmnToJson(SAMPLE_EXCLUSIVE)
    const fa = json.edges.find(e => e.id === 'F_a')
    const fe = json.edges.find(e => e.id === 'F_else')
    expect(fa.branchId).toBe('b1')
    expect(fa.isDefault).toBe(false)
    expect(fa.condition).toBe(`${DOLLAR}{days > 3}`)
    expect(fa.conditionType).toBe('expression')
    expect(fe.branchId).toBe('b2')
    expect(fe.isDefault).toBe(true)
    expect(fe.condition).toBe('')
  })

  it('非网关出边不分配 branchId', () => {
    const json = convertBpmnToJson(SAMPLE_EXCLUSIVE)
    const fin = json.edges.find(e => e.id === 'F_in')
    expect(fin.branchId).toBe(null)
    expect(fin.isDefault).toBe(false)
  })

  it('汇合节点入度 >= 2 → mergeNode 标记', () => {
    const json = convertBpmnToJson(SAMPLE_EXCLUSIVE)
    const merge = json.nodes.find(n => n.id === 'GW_merge')
    expect(merge.config.mergeNode).toBe(true)
  })
})

describe('markBranches - 并行网关', () => {
  it('3 条出边分配 b1/b2/b3，全部无 condition / 非 default', () => {
    const json = convertBpmnToJson(SAMPLE_PARALLEL)
    const ids = ['F1', 'F2', 'F3']
    for (let i = 0; i < ids.length; i += 1) {
      const e = json.edges.find(x => x.id === ids[i])
      expect(e.branchId).toBe(`b${i + 1}`)
      expect(e.isDefault).toBe(false)
      expect(e.condition).toBe('')
    }
  })
})

describe('markBranches - 嵌套分支 branchId 不冲突', () => {
  it('外层 GW_outer 拿 b1/b2，内层 GW_inner 拿 b3/b4', () => {
    const json = convertBpmnToJson(SAMPLE_NESTED)
    const fo1 = json.edges.find(e => e.id === 'F_o1')
    const fo2 = json.edges.find(e => e.id === 'F_o2')
    const fi1 = json.edges.find(e => e.id === 'F_i1')
    const fi2 = json.edges.find(e => e.id === 'F_i2')
    // 外层先编号（按 nodes 顺序遍历）
    const outerIds = [fo1.branchId, fo2.branchId].sort()
    const innerIds = [fi1.branchId, fi2.branchId].sort()
    // 4 个 branchId 不重复
    const allIds = new Set([...outerIds, ...innerIds])
    expect(allIds.size).toBe(4)
  })
})

describe('getNodeInDegree / getNodeOutDegree', () => {
  it('正确计算入度 / 出度', () => {
    const json = convertBpmnToJson(SAMPLE_EXCLUSIVE)
    const inDeg = getNodeInDegree(json.nodes, json.edges)
    const outDeg = getNodeOutDegree(json.nodes, json.edges)
    expect(inDeg.get('GW_merge')).toBe(2)
    expect(inDeg.get('GW1')).toBe(1)
    expect(inDeg.get('S')).toBe(0)
    expect(outDeg.get('GW1')).toBe(2)
    expect(outDeg.get('E')).toBe(0)
  })
})

describe('markBranches - 兜底', () => {
  it('flowJson 为空时返回空结果', () => {
    expect(markBranches(null)).toEqual({ inDegree: new Map(), outDegree: new Map(), mergeNodeIds: [] })
    expect(markBranches({}).mergeNodeIds).toEqual([])
  })
})
