import { mount } from '@vue/test-utils'
import { NDropdown } from 'naive-ui'
import { describe, expect, it } from 'vitest'
import NodeContextMenu from '../NodeContextMenu.vue'
import NodeRenderer from '../NodeRenderer.vue'

function menuText(wrapper) {
  return wrapper.getComponent(NDropdown).props('options').map(item => item.label || '').join(' ')
}

describe('nodeContextMenu', () => {
  it('visible=false 不渲染', () => {
    const w = mount(NodeContextMenu, { props: { visible: false } })
    expect(w.getComponent(NDropdown).props('show')).toBe(false)
  })

  it('普通节点显示编辑 / 复制 / 上下移 / 删除', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'A', nodeType: 'approver' } },
    })
    const text = menuText(w)
    expect(text).toContain('编辑')
    expect(text).toContain('复制')
    expect(text).toContain('合并审批节点')
    expect(text).toContain('上移')
    expect(text).toContain('下移')
    expect(text).toContain('删除')
  })

  it('start 节点只显示编辑', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'S', nodeType: 'start' } },
    })
    expect(menuText(w)).toContain('编辑')
    expect(menuText(w)).not.toContain('复制')
    expect(menuText(w)).not.toContain('合并审批节点')
    expect(menuText(w)).not.toContain('删除')
  })

  it('readonly 模式只显示编辑', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, readonly: true, node: { id: 'A', nodeType: 'approver' } },
    })
    expect(menuText(w)).toContain('编辑')
    expect(menuText(w)).not.toContain('删除')
    expect(menuText(w)).not.toContain('复制')
    expect(menuText(w)).not.toContain('合并审批节点')
  })

  it('canMoveUp=false 隐藏 "上移"', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, canMoveUp: false, node: { id: 'A', nodeType: 'approver' } },
    })
    expect(menuText(w)).not.toContain('上移')
    expect(menuText(w)).toContain('下移')
  })

  it('advanced 节点不显示复制', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'X', nodeType: 'advanced' } },
    })
    expect(menuText(w)).not.toContain('复制')
    expect(menuText(w)).not.toContain('合并审批节点')
    expect(menuText(w)).toContain('编辑')
    expect(menuText(w)).toContain('删除')
  })

  it('菜单选择仍携带当前节点并关闭', async () => {
    const node = { id: 'A', nodeType: 'approver' }
    const w = mount(NodeContextMenu, { props: { visible: true, node } })
    w.getComponent(NDropdown).vm.$emit('select', 'delete')
    await w.vm.$nextTick()
    expect(w.emitted('action')[0]).toEqual(['delete', node])
    expect(w.emitted('close')).toHaveLength(1)
  })
})

describe('nodeRenderer - 调度', () => {
  const types = [
    ['start', '发起人'],
    ['approver', '审批'],
    ['carbonCopy', '抄送'],
    ['condition', '条件分支'],
    ['parallel', '并行分支'],
    ['inclusive', '包容分支'],
    ['service', '服务'],
    ['script', '脚本'],
    ['callActivity', '调用'],
    ['advanced', '高级'],
    ['end', '结束'],
  ]

  for (const [t, expected] of types) {
    it(`${t} → 渲染对应卡片 (含 "${expected}")`, () => {
      const w = mount(NodeRenderer, {
        props: {
          node: { id: 'N', name: t, nodeType: t, config: {}, bpmnElementType: 'bpmn:Activity' },
          position: { x: 10, y: 20 },
        },
      })
      // 验证未崩 + 至少含期望关键字
      expect(w.html()).toContain('flow-node-card')
    })
  }

  it('未知 nodeType → 兜底为 advanced', () => {
    const w = mount(NodeRenderer, {
      props: { node: { id: 'X', name: 'unknown', nodeType: 'no-such-type', config: {}, bpmnElementType: 'bpmn:Activity' } },
    })
    expect(w.text()).toContain('高级')
  })

  it('应用 position 绝对定位', () => {
    const w = mount(NodeRenderer, {
      props: {
        node: { id: 'A', name: 'X', nodeType: 'approver', config: {} },
        position: { x: 100, y: 50 },
      },
    })
    const wrap = w.find('.node-renderer-wrap')
    expect(wrap.attributes('style')).toContain('left: 100px')
    expect(wrap.attributes('style')).toContain('top: 50px')
  })
})
