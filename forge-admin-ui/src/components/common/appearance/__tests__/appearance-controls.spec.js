import { mount } from '@vue/test-utils'
import { NCollapse, NCollapseItem, NColorPicker, NSwitch } from 'naive-ui'
import { describe, expect, it } from 'vitest'
import { defaultThemeConfig } from '@/config/theme.config'
import AppearanceThemeEditor from '../AppearanceThemeEditor.vue'
import LayoutPicker from '../LayoutPicker.vue'

const components = { NCollapse, NCollapseItem, NColorPicker, NSwitch }
describe('共用外观控件', () => {
  it('默认三个基础色，高级项收起，预设更新整个方案而非单个颜色', async () => {
    const wrapper = mount(AppearanceThemeEditor, {
      props: { modelValue: defaultThemeConfig, layout: 'normal' },
      global: { components },
    })
    try {
      expect(wrapper.findAllComponents(NColorPicker)).toHaveLength(3)
      expect(wrapper.find('.manual-mode').exists()).toBe(false)
      await wrapper.findAll('.theme-presets button')[1].trigger('click')
      const theme = wrapper.emitted('update:modelValue')[0][0]
      expect(theme.navigationMode).toBe('auto')
      expect(theme.header.textColor).toBe('#FFFFFF')
      expect(theme.sideMenu.textColorActive).toBe(theme.sideMenu.iconColorActive)
      await wrapper.setProps({ layout: 'bento' })
      expect(wrapper.findAllComponents(NColorPicker)).toHaveLength(2)
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      await wrapper.setProps({ modelValue: { ...defaultThemeConfig, navigationMode: 'custom' } })
      expect(wrapper.text()).not.toContain('顶栏文字 / 工具')
      expect(wrapper.findAllComponents(NColorPicker)).toHaveLength(6)
      await wrapper.setProps({ layout: 'empty' })
      expect(wrapper.findAllComponents(NColorPicker)).toHaveLength(1)
      expect(wrapper.findComponent(NCollapse).exists()).toBe(false)
    }
    finally { wrapper.unmount() }
  })
  it('个人可选空白，租户默认不提供空白，选择事件立即返回新布局', async () => {
    const wrapper = mount(LayoutPicker, { props: { modelValue: 'normal' } })
    try {
      const empty = wrapper.findAll('button').find(button => button.text().includes('空布局'))
      expect(empty).toBeTruthy()
      await empty.trigger('click')
      expect(wrapper.emitted('update:modelValue')[0]).toEqual(['empty'])
      await wrapper.setProps({ tenant: true })
      expect(wrapper.text()).not.toContain('空布局')
      expect(wrapper.text()).not.toContain('应用门户布局')
    }
    finally { wrapper.unmount() }
  })
  it('深浅预览只切换样例，不修改草稿或发出主题更新', async () => {
    const source = {
      ...defaultThemeConfig,
      headerDark: { ...defaultThemeConfig.headerDark, backgroundColor: '#061917' },
    }
    const original = JSON.stringify(source)
    const wrapper = mount(AppearanceThemeEditor, {
      props: { modelValue: source, layout: 'normal' },
      global: { components },
    })
    try {
      await wrapper.findAll('.preview-modes button')[1].trigger('click')
      expect(wrapper.findAll('.preview-modes button')[1].attributes('aria-pressed')).toBe('true')
      expect(wrapper.find('.theme-sample').attributes('style')).toContain('--sample-header: #061917')
      expect(wrapper.emitted('update:modelValue')).toBeUndefined()
      expect(JSON.stringify(source)).toBe(original)
      await wrapper.findAll('.preview-modes button')[0].trigger('click')
      expect(wrapper.find('.theme-sample').attributes('style')).toContain('--sample-header: #ffffff')
      expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    }
    finally { wrapper.unmount() }
  })
})
