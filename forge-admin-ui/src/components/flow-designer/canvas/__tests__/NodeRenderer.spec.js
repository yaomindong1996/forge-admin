import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NodeContextMenu from '../NodeContextMenu.vue'
import NodeRenderer from '../NodeRenderer.vue'

describe('nodeContextMenu', () => {
  it('visible=false 不渲染', () => {
    const w = mount(NodeContextMenu, { props: { visible: false } })
    expect(w.find('.node-context-menu').exists()).toBe(false)
  })

  it('普通节点显示编辑 / 复制 / 上下移 / 删除', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'A', nodeType: 'approver' } },
    })
    const text = w.text()
    expect(text).toContain('编辑')
    expect(text).toContain('复制')
    expect(text).toContain('上移')
    expect(text).toContain('下移')
    expect(text).toContain('删除')
  })

  it('start 节点只显示编辑', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'S', nodeType: 'start' } },
    })
    expect(w.text()).toContain('编辑')
    expect(w.text()).not.toContain('复制')
    expect(w.text()).not.toContain('删除')
  })

  it('readonly 模式只显示编辑', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, readonly: true, node: { id: 'A', nodeType: 'approver' } },
    })
    expect(w.text()).toContain('编辑')
    expect(w.text()).not.toContain('删除')
    expect(w.text()).not.toContain('复制')
  })

  it('canMoveUp=false 隐藏 "上移"', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, canMoveUp: false, node: { id: 'A', nodeType: 'approver' } },
    })
    expect(w.text()).not.toContain('上移')
    expect(w.text()).toContain('下移')
  })

  it('advanced 节点不显示复制', () => {
    const w = mount(NodeContextMenu, {
      props: { visible: true, node: { id: 'X', nodeType: 'advanced' } },
    })
    expect(w.text()).not.toContain('复制')
    expect(w.text()).toContain('编辑')
    expect(w.text()).toContain('删除')
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
