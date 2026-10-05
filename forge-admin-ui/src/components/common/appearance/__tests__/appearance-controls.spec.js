import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
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

  it('高级深色编辑写入正确分组，基础设置仍保持简洁', async () => {
    const source = { ...defaultThemeConfig, navigationMode: 'custom', extension: { density: 'compact' } }
    const wrapper = mount(AppearanceThemeEditor, {
      props: { modelValue: source, layout: 'normal' },
      global: { components },
    })
    try {
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      await wrapper.findAll('.custom-theme-mode button')[1].trigger('click')
      expect(wrapper.emitted('update:modelValue')).toBeUndefined()
      expect(wrapper.find('.dark-base-fields').text()).toContain('深色顶栏背景')
      const fields = wrapper.findAllComponents(NColorPicker)
      await fields[3].vm.$emit('update:value', '#061917')
      const baseUpdate = wrapper.emitted('update:modelValue')[0][0]
      expect(baseUpdate.headerDark.backgroundColor).toBe('#061917')
      expect(baseUpdate.header).toEqual(source.header)
      expect(baseUpdate.extension).toEqual(source.extension)
      // 修改基础色会回到自动配色；显式恢复手动模式后再验证高级文字编辑。
      await wrapper.setProps({ modelValue: baseUpdate })
      await wrapper.findComponent(NSwitch).vm.$emit('update:value', true)
      const manualUpdate = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(manualUpdate.navigationMode).toBe('custom')
      expect(manualUpdate.navigationModeDark).toBe('custom')
      await wrapper.setProps({ modelValue: manualUpdate })
      const customFields = wrapper.findAllComponents(NColorPicker)
      await customFields[5].vm.$emit('update:value', '#E5E7EB')
      const textUpdate = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(textUpdate.headerDark.textColor).toBe('#E5E7EB')
      expect(textUpdate.headerDark.brandTitleTextColor).toBe('#E5E7EB')
      expect(textUpdate.header).toEqual(source.header)
      await customFields[6].vm.$emit('update:value', '#7DB7FF')
      const activeUpdate = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(activeUpdate.topMenuDark.iconActiveColor).toBe('#7DB7FF')
      expect(activeUpdate.topMenuDark.textColorActiveHorizontal).toBe('#7DB7FF')
      expect(activeUpdate.topMenu).toEqual(source.topMenu)
      await wrapper.setProps({ layout: 'simple' })
      expect(wrapper.find('.dark-base-fields').text()).not.toContain('深色顶栏背景')
    }
    finally { wrapper.unmount() }
  })

  it('高级手动开关按当前模式回显，进入手动时保留实际预览颜色', async () => {
    const wrapper = mount(AppearanceThemeEditor, {
      props: { modelValue: { ...defaultThemeConfig, navigationModeDark: 'custom' }, layout: 'normal' },
      global: { components },
    })
    try {
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      expect(wrapper.findComponent(NSwitch).props('value')).toBe(false)
      const original = wrapper.find('.theme-sample').attributes('style')
      await wrapper.findComponent(NSwitch).vm.$emit('update:value', true)
      const next = wrapper.emitted('update:modelValue')[0][0]
      await wrapper.setProps({ modelValue: next })
      expect(wrapper.find('.theme-sample').attributes('style')).toBe(original)
      expect(next.navigationModeDark).toBe('custom')
      await wrapper.findAll('.custom-theme-mode button')[1].trigger('click')
      expect(wrapper.findComponent(NSwitch).props('value')).toBe(true)
      await wrapper.findComponent(NSwitch).vm.$emit('update:value', false)
      const updated = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(updated.navigationMode).toBe('custom')
      expect(updated.navigationModeDark).toBe('auto')
      expect(updated.header).toEqual(next.header)
    }
    finally { wrapper.unmount() }
  })

  it('真实取色器 HEX 输入提交到深色草稿，不覆盖浅色背景', async () => {
    const wrapper = mount(AppearanceThemeEditor, {
      attachTo: document.body,
      props: { modelValue: defaultThemeConfig, layout: 'normal' },
      global: { components },
    })
    try {
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      await wrapper.findAll('.custom-theme-mode button')[1].trigger('click')
      await wrapper.find('.dark-base-fields .n-color-picker').trigger('click')
      await flushPromises()
      const input = new DOMWrapper(document.querySelector('.n-color-picker-panel input'))
      await input.setValue('#061917')
      await flushPromises()
      const updated = wrapper.emitted('update:modelValue')[0][0]
      expect(updated.headerDark.backgroundColor).toBe('#061917')
      expect(updated.header.backgroundColor).toBe(defaultThemeConfig.header.backgroundColor)
      expect(wrapper.find('.theme-sample').attributes('style')).toContain('--sample-header: #061917')
    }
    finally { wrapper.unmount() }
  })
})
