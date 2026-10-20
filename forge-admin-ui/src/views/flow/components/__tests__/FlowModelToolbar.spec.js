import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FlowModelToolbar from '../FlowModelToolbar.vue'

describe('模型列表工具栏', () => {
  it('搜索输入与提交分离，新增入口保持一个主操作', async () => {
    const wrapper = mount(FlowModelToolbar)
    await wrapper.get('input').setValue('合同')
    expect(wrapper.emitted('update:modelName')[0]).toEqual(['合同'])
    expect(wrapper.emitted('search')).toBeUndefined()
    await wrapper.get('input').trigger('keyup', { key: 'Enter' })
    expect(wrapper.emitted('search')).toHaveLength(1)
    expect(wrapper.text()).toContain('新增模型')
    expect(wrapper.text()).not.toContain('调整本页排序')
    wrapper.unmount()
  })
  it('排序时锁定筛选，显示保存/取消，不叠加新增按钮', () => {
    const wrapper = mount(FlowModelToolbar, { props: { sortMode: true } })
    expect(wrapper.get('input').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('保存排序')
    expect(wrapper.text()).toContain('取消排序')
    expect(wrapper.text()).not.toContain('新增模型')
    wrapper.unmount()
  })
})
