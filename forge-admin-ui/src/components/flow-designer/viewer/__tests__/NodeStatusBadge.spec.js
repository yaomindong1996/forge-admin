import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NodeStatusBadge from '../NodeStatusBadge.vue'

describe('nodeStatusBadge', () => {
  it('无 status 时不渲染', () => {
    const w = mount(NodeStatusBadge)
    expect(w.find('.node-status-badge').exists()).toBe(false)
  })

  it('各 status 渲染对应文案 + 类名', () => {
    const cases = [
      { status: 'completed', label: '已完成', cls: 'text-success' },
      { status: 'running', label: '审批中', cls: 'text-info' },
      { status: 'pending', label: '待办', cls: 'text-gray-500' },
      { status: 'rejected', label: '已驳回', cls: 'text-error' },
      { status: 'skipped', label: '已跳过', cls: 'text-gray-400' },
    ]
    for (const { status, label, cls } of cases) {
      const w = mount(NodeStatusBadge, { props: { status } })
      expect(w.text()).toContain(label)
      expect(w.attributes('class')).toContain(cls)
    }
  })

  it('size 影响样式', () => {
    const w = mount(NodeStatusBadge, { props: { status: 'completed', size: 'tiny' } })
    expect(w.attributes('class')).toContain('text-[10px]')
  })
})
