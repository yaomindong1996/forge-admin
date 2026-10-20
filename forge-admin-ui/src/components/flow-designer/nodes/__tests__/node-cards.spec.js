import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { defineComponent } from 'vue'
import AdvancedNode from '../AdvancedNode.vue'
import ApproverNode from '../ApproverNode.vue'
import BranchNode from '../BranchNode.vue'
import CarbonCopyNode from '../CarbonCopyNode.vue'
import EndNode from '../EndNode.vue'
import NodeCard from '../NodeCard.vue'
import StartNode from '../StartNode.vue'

describe('nodeCard - 基础渲染', () => {
  it('选中状态渲染高亮 class', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, selected: true },
      slots: { default: 'body' },
    })
    expect(w.find('.flow-node-card').classes()).toContain('is-selected')
  })

  it('readonly 模式不显示删除按钮', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, readonly: true },
    })
    expect(w.find('button[aria-label="删除节点"]').exists()).toBe(false)
  })

  it('readonly 模式隐藏流程配置摘要和标题附加徽章', () => {
    const w = mount(NodeCard, {
      props: {
        node: { id: 'A', name: '部门经理审批', nodeType: 'approver' },
        readonly: true,
        subtitle: 'SPEL：$' + '{deptManager}',
      },
      slots: {
        'title-extra': '<span class="config-badge">会签</span>',
      },
    })
    expect(w.text()).toContain('部门经理审批')
    expect(w.text()).not.toContain('SPEL')
    expect(w.text()).not.toContain('会签')
    expect(w.find('.flow-node-summary').exists()).toBe(false)
    expect(w.find('.config-badge').exists()).toBe(false)
  })

  it('start / end 节点不显示删除按钮（即使非 readonly）', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'S', name: '发起', nodeType: 'start' }, deletable: false },
    })
    expect(w.find('button[aria-label="删除节点"]').exists()).toBe(false)
  })

  it('普通节点更多入口可直接打开原有操作菜单', async () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' } },
    })
    await w.find('button[aria-label="节点更多操作"]').trigger('click')
    expect(w.emitted('contextMenu')[0][0].node.id).toBe('A')
    expect(w.emitted('click')).toBeUndefined()
  })

  it('status 状态徽章显示', () => {
    const w = mount(NodeCard, {
      props: { node: { id: 'A', name: 'X', nodeType: 'approver' }, status: 'completed' },
    })
    expect(w.text()).toContain('已完成')
    expect(w.find('.flow-node-meta').exists()).toBe(true)
  })

  it('右键事件可通过模板 @context-menu 监听', async () => {
    const Parent = defineComponent({
      components: { NodeCard },
      data: () => ({
        node: { id: 'A', name: 'X', nodeType: 'approver' },
        received: null,
      }),
      template: '<NodeCard :node="node" @context-menu="received = $event" />',
    })
    const w = mount(Parent)
    await w.find('.flow-node-card').trigger('contextmenu')
    expect(w.vm.received?.node?.id).toBe('A')
  })
})

describe('startNode', () => {
  it('渲染节点名和固定发起人摘要', () => {
    const w = mount(StartNode, {
      props: { node: { id: 'S', name: '发起人', nodeType: 'start', config: { initiator: 'initiator', formKey: 'leaveForm' } } },
    })
    expect(w.text()).toContain('发起人')
    expect(w.text()).toContain('系统自动记录发起人')
    expect(w.text()).not.toContain('leaveForm')
  })

  it('未配置 config 也能渲染', () => {
    const w = mount(StartNode, {
      props: { node: { id: 'S', name: '发起', nodeType: 'start', config: {} } },
    })
    expect(w.text()).toContain('发起')
  })
})

describe('endNode', () => {
  it('pill 变体渲染节点名', () => {
    const w = mount(EndNode, {
      props: { node: { id: 'E', name: '流程结束', nodeType: 'end', config: { endType: 'normal' } } },
    })
    expect(w.text()).toContain('流程结束')
  })

  it('terminate endType 也能渲染', () => {
    const w = mount(EndNode, {
      props: { node: { id: 'E', name: '终止', nodeType: 'end', config: { endType: 'terminate' } } },
    })
    expect(w.text()).toContain('终止')
  })
})

describe('approverNode', () => {
  it('static 变量直接显示中文', () => {
    const w = mount(ApproverNode, {
      props: { node: { id: 'A', name: '审批', nodeType: 'approver', config: { taskType: 'assignee', assignee: '$' + '{deptManager}' } } },
    })
    expect(w.text()).toContain('部门主管')
  })

  it('multiInstance 显示 "会签" 徽章', () => {
    const w = mount(ApproverNode, {
      props: { node: { id: 'A', name: '审批', nodeType: 'approver', config: {
        taskType: 'assignee',
        assignee: '$' + '{deptManager}',
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
    expect(w.text()).toContain('2 条条件分支')
  })

  it('parallel 显示并行', () => {
    const w = mount(BranchNode, {
      props: { node: { id: 'GW', name: 'p', nodeType: 'parallel', config: {} }, outgoingCount: 3 },
    })
    expect(w.text()).toContain('并行分支')
    expect(w.text()).toContain('3 条并行分支')
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
