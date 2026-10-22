import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PageWidgetRenderer from '../PageWidgetRenderer.vue'

describe('PageWidgetRenderer html-tag', () => {
  it('renders htmlContent via v-html and applies style from attributes JSON', () => {
    const wrapper = mount(PageWidgetRenderer, {
      props: {
        componentKey: 'html-tag',
        propsData: {
          tagName: 'div',
          renderMode: 'html',
          htmlContent: '<strong style="color:#3370ff">标题</strong><p>正文</p>',
          attributesText: JSON.stringify({
            class: 'html-panel',
            style: 'padding:12px;background:#f5f8ff;',
            'aria-label': '说明区块',
          }),
        },
        readonly: true,
      },
    })
    const root = wrapper.find('.html-widget')
    expect(root.exists()).toBe(true)
    expect(root.classes()).toContain('html-panel')
    expect(String(root.attributes('style') || '').replace(/\s/g, '')).toContain('padding:12px')
    expect(root.attributes('aria-label')).toBe('说明区块')
    expect(root.html()).toContain('<strong')
    expect(root.html()).toContain('标题')
    expect(root.html()).toContain('<p>')
    expect(wrapper.find('.html-meta').exists()).toBe(false)
  })
})
