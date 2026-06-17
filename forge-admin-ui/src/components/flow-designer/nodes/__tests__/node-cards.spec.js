import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AdvancedNode from '../AdvancedNode.vue'
import ApproverNode from '../ApproverNode.vue'
import BranchNode from '../BranchNode.vue'
import CarbonCopyNode from '../CarbonCopyNode.vue'
import EndNode from '../EndNode.vue'
import NodeCard from '../NodeCard.vue'
import StartNode from '../StartNode.vue'

describe('nodeCard - 基础渲染', () => {
  it('选中状态高亮 ring', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, selected: true },
      slots: { default: 'body' },
    })
    expect(w.classes().some(c => c.includes('ring-primary'))).toBe(true)
  })

  it('readonly 模式不显示删除按钮', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, readonly: true },
    })
    expect(w.find('button[aria-label="删除节点"]').exists()).toBe(false)
  })

  it('start / end 节点不显示删除按钮（即使非 readonly）', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'S', name: '发起', nodeType: 'start' } },
    })
    expect(w.find('button[aria-label="删除节点"]').exists()).toBe(false)
  })

  it('普通节点显示删除按钮', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' } },
    })
    expect(w.find('button[aria-label="删除节点"]').exists()).toBe(true)
  })

  it('status 状态徽章显示', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, status: 'completed' },
    })
    expect(w.text()).toContain('已完成')
  })
})

describe('startNode', () => {
  it('显示 initiator + 表单', () => {
    const w = mount(StartNode, {
      props: { node: { id: 'S', name: '发起', nodeType: 'start', config: { initiator: 'initiator', formKey: 'leaveForm' } } },
    })
    expect(w.text()).toContain('initiator')
    expect(w.text()).toContain('leaveForm')
  })

  it('未配置时占位', () => {
    const w = mount(StartNode, {
      props: { node: { id: 'S', name: '发起', nodeType: 'start', config: {} } },
    })
    expect(w.text()).toContain('点击配置发起人')
  })
})

describe('endNode', () => {
  it('endType=normal 显示 "正常结束"', () => {
    const w = mount(EndNode, {
      props: { node: { id: 'E', name: '结束', nodeType: 'end', config: { endType: 'normal' } } },
    })
    expect(w.text()).toContain('正常结束')
  })

  it('endType=terminate 显示 "强制终止"', () => {
    const w = mount(EndNode, {
      props: { node: { id: 'E', name: '结束', nodeType: 'end', config: { endType: 'terminate' } } },
    })
    expect(w.text()).toContain('强制终止流程')
  })
})

describe('approverNode', () => {
  it('static 变量直接显示中文', () => {
    const w = mount(ApproverNode, {
      props: { node: { id: 'A', name: '审批', nodeType: 'approver', config: { taskType: 'assignee', assignee: '${deptManager}' } } },
    })
    expect(w.text()).toContain('部门主管')
  })

  it('multiInstance 显示 "会签" 徽章', () => {
    const w = mount(ApproverNode, {
      props: { node: { id: 'A', name: '审批', nodeType: 'approver', config: {
        taskType: 'assignee',
        assignee: '${deptManager}',
        multiInstanceType: 'parallel',
        completionCondition: 'all',
      } } },
    })
    expect(w.text()).toContain('会签')
  })
})

describe('carbonCopyNode', () => {
  it('多人抄送显示前 3 个名字', () => {
    const w = mount(CarbonCopyNode, {
      props: { node: { id: 'C', name: '抄送', nodeType: 'carbonCopy', config: {
        candidateUsers: ['1', '2', '3', '4'],
        candidateUserNames: ['张三', '李四', '王五', '赵六'],
      } } },
    })
    expect(w.text()).toContain('抄送 4 人')
    expect(w.text()).toContain('张三')
  })
})

describe('branchNode', () => {
  it('condition + 分支数', () => {
    const w = mount(BranchNode, {
      props: { node: { id: 'GW', name: '分支', nodeType: 'condition', config: {} }, outgoingCount: 2 },
    })
    expect(w.text()).toContain('条件分支')
    expect(w.text()).toContain('2 条分支')
  })

  it('parallel 显示并行', () => {
    const w = mount(BranchNode, {
      props: { node: { id: 'GW', name: 'p', nodeType: 'parallel', config: {} }, outgoingCount: 3 },
    })
    expect(w.text()).toContain('并行分支')
    expect(w.text()).toContain('3 条并行')
  })

  it('inclusive 显示包容', () => {
    const w = mount(BranchNode, {
      props: { node: { id: 'GW', name: 'i', nodeType: 'inclusive', config: {} }, outgoingCount: 0 },
    })
    expect(w.text()).toContain('包容分支')
    expect(w.text()).toContain('点击配置')
  })
})

describe('advancedNode', () => {
  it('显示 bpmnElementType + "高级" 徽章', () => {
    const w = mount(AdvancedNode, {
      props: { node: { id: 'X', name: '中间', nodeType: 'advanced', bpmnElementType: 'bpmn:IntermediateCatchEvent', config: {} } },
    })
    expect(w.text()).toContain('IntermediateCatchEvent')
    expect(w.text()).toContain('高级')
  })
})
