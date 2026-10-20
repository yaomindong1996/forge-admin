import { makeEdge } from '../../composables/flow-designer-helpers.js'
import { buildNode } from '../../constants/default-configs.js'

/** 用户截图同构的合成流程，不包含真实账号或任务数据。 */
export function approvalMergeFixture(gatewayType = 'condition', branchCount = 2) {
  const nodes = [
    buildNode('start', { id: 'S', name: '发起人' }),
    buildNode(gatewayType, { id: 'G', name: '业务条件' }),
    buildNode('end', { id: 'E', name: '结束' }),
  ]
  const edges = [makeEdge('SG', 'S', 'G')]
  // 模拟 XML 解析器 markBranches 的汇合标记，隔离画布也要与真实导入一致。
  nodes[2].config.mergeNode = true
  for (let i = 1; i <= branchCount; i += 1) {
    const head = buildNode('approver', { id: `H${i}`, name: `分支${i}经办` })
    const tail = buildNode('approver', { id: `A${i}`, name: `公共审批${i}` })
    tail.config = {
      ...tail.config,
      taskType: 'candidateUsers',
      candidateUsers: ['fixture-admin'],
      candidateUserNames: ['管理员（测试）'],
    }
    nodes.push(head, tail)
    edges.push(makeEdge(`GH${i}`, 'G', head.id, {
      branchId: `b${i}`,
      isDefault: i === branchCount && gatewayType !== 'parallel',
      condition: i === branchCount ? '' : '$' + `{amount >= ${i * 5000}}`,
    }), makeEdge(`HA${i}`, head.id, tail.id), makeEdge(`AE${i}`, tail.id, 'E'))
  }
  if (gatewayType !== 'parallel')
    nodes[1].config.defaultFlowId = `GH${branchCount}`
  return { processId: 'MergeFixture', processName: '分支汇合测试', config: { rejectStrategy: 'MANUAL' }, nodes, edges }
}
