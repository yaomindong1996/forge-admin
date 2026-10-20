import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FlowDesignToolbar from '../FlowDesignToolbar.vue'

describe('流程设计工具栏', () => {
  it('显示真实模型名称与状态，不使用效果图示例名称', () => {
    const w = mount(FlowDesignToolbar, { props: { name: '合同审批', status: { label: '已发布', type: 'success' } } })
    expect(w.get('h1').text()).toBe('合同审批')
    expect(w.text()).toContain('已发布')
    expect(w.text()).not.toContain('费用报销')
    w.unmount()
  })

  it('工作区页签和全部操作分别发出原有动作', async () => {
    const w = mount(FlowDesignToolbar)
    const actions = { 返回: 'back', 更改记录: 'history', AI辅助: 'ai', 保存草稿: 'save', 发布流程: 'deploy' }
    for (const [label, event] of Object.entries(actions)) {
      const button = w.findAll('button').find(item => item.text().includes(label))
      await button.trigger('click')
      expect(w.emitted(event)).toHaveLength(1)
    }
    await w.get('.flow-design-tabs button:last-child').trigger('click')
    expect(w.emitted('mode')[0]).toEqual(['settings'])
    await w.setProps({ mode: 'settings' })
    expect(w.get('.flow-design-tabs button:last-child').attributes('aria-current')).toBe('page')
    w.unmount()
  })
})
