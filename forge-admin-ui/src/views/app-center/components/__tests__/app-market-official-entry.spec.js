import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppMarketPanel from '../AppMarketPanel.vue'

const stubs = {
  NTabs: true,
  NTab: true,
  NInput: true,
  NIcon: true,
  NSelect: true,
  NEmpty: true,
  IconRenderer: true,
  NButton: {
    props: ['href', 'tag'],
    template: '<a :href="href"><slot /><slot name="icon" /></a>',
  },
}

describe('app market official entry', () => {
  it('renders colorful hero entry and compact template cards', () => {
    const wrapper = mount(AppMarketPanel, { global: { stubs } })
    expect(wrapper.find('.market-hero').exists()).toBe(true)
    expect(wrapper.text()).toContain('打开应用市场')
    expect(wrapper.text()).toContain('导入应用')
    expect(wrapper.find('a').attributes('href')).toContain('/templates?kind=app')
    expect(wrapper.findAll('.market-item').length).toBeGreaterThan(0)
    expect(wrapper.find('.market-item').attributes('style')).toContain('--market-ink')
    expect(wrapper.text()).toContain('立即启用')
  })

  it('takes unmatched local search to the official app market with the query', async () => {
    const wrapper = mount(AppMarketPanel, {
      global: {
        stubs: {
          ...stubs,
          NEmpty: { template: '<div class="empty"><slot name="extra" /></div>' },
        },
      },
    })
    wrapper.vm.keyword = '采购单'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('a[href*="q="]').attributes('href')).toContain('/templates?kind=app')
    expect(wrapper.text()).toContain('到官网应用市场搜索')
  })
})
