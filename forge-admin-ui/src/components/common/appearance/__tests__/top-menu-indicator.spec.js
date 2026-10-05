import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { NCollapse, NCollapseItem, NColorPicker, NSwitch } from 'naive-ui'
import { describe, expect, it } from 'vitest'
import { defaultThemeConfig } from '@/config/theme.config'
import { updateNavigationBases } from '@/utils/navigation-theme'
import AppearanceThemeEditor from '../AppearanceThemeEditor.vue'
import TopMenuIndicatorControl from '../TopMenuIndicatorControl.vue'

const components = { NCollapse, NCollapseItem, NColorPicker, NSwitch }
const source = () => structuredClone(defaultThemeConfig)

describe('顶部菜单横条编辑', () => {
  it.each(['top-menu', 'top-side-menu', 'business-workbench'])('%s 高级区可设置横条且不关闭自动文字配色', async (layout) => {
    const model = source()
    const original = JSON.stringify(model)
    const wrapper = mount(AppearanceThemeEditor, { props: { modelValue: model, layout }, global: { components } })
    try {
      expect(wrapper.findAllComponents(NColorPicker)).toHaveLength(3)
      expect(wrapper.findComponent(TopMenuIndicatorControl).exists()).toBe(false)
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      const indicator = wrapper.findComponent(TopMenuIndicatorControl)
      expect(indicator.findComponent(NColorPicker).props('disabled')).toBe(true)
      await indicator.findComponent(NSwitch).vm.$emit('update:value', false)
      const custom = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(custom.navigationMode).toBe('auto')
      expect(custom.topMenu.activeBarColor).toBe('#4242f7')
      expect(custom.header).toEqual(model.header)
      await wrapper.setProps({ modelValue: custom })
      await indicator.findComponent(NColorPicker).vm.$emit('update:value', '#FF7D00')
      const updated = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(updated.topMenu.activeBarColor).toBe('#FF7D00')
      expect(updated.topMenuDark).toEqual(model.topMenuDark)
      expect(updated.navigationMode).toBe('auto')
      await wrapper.setProps({ modelValue: updated })
      expect(wrapper.find('.theme-sample').attributes('style')).toContain('--sample-active-bar: #ff7d00')
      expect(JSON.stringify(model)).toBe(original)
    }
    finally { wrapper.unmount() }
  })

  it('深色只修改深色横条，恢复跟随与以后改品牌色均不会冻结', async () => {
    const model = { ...source(), topMenu: { activeBarColor: '#FF7D00' }, extension: { version: 3 } }
    const wrapper = mount(TopMenuIndicatorControl, { props: { modelValue: model, dark: true }, global: { components } })
    try {
      await wrapper.findComponent(NSwitch).vm.$emit('update:value', false)
      const custom = wrapper.emitted('update:modelValue').at(-1)[0]
      await wrapper.setProps({ modelValue: custom })
      await wrapper.findComponent(NColorPicker).vm.$emit('update:value', '#7DB7FF')
      const updated = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(updated.topMenuDark.activeBarColor).toBe('#7DB7FF')
      expect(updated.topMenu).toEqual(model.topMenu)
      expect(updated.extension).toEqual(model.extension)
      await wrapper.setProps({ modelValue: updated })
      await wrapper.findComponent(NSwitch).vm.$emit('update:value', true)
      const reset = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(reset.topMenuDark.activeBarColor).toBe('')
      await wrapper.setProps({ modelValue: updateNavigationBases(reset, { primary: '#0E8F7E' }, true) })
      expect(wrapper.findComponent(NSwitch).props('value')).toBe(true)
      expect(wrapper.findComponent(NColorPicker).props('value')).toBe('#0e8f7e')
      expect(wrapper.findComponent(NColorPicker).props('disabled')).toBe(true)
    }
    finally { wrapper.unmount() }
  })

  it('进入手动或切浅深预览不把默认跟随写成固定横条', async () => {
    const wrapper = mount(AppearanceThemeEditor, {
      props: { modelValue: source(), layout: 'business-workbench' },
      global: { components },
    })
    try {
      await wrapper.find('.n-collapse-item__header-main').trigger('click')
      await wrapper.find('.manual-mode').findComponent(NSwitch).vm.$emit('update:value', true)
      const next = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(next.topMenu.activeBarColor).toBe('')
      expect(next.topMenuDark.activeBarColor).toBe('')
      await wrapper.setProps({ modelValue: next })
      await wrapper.findAll('.custom-theme-mode button')[1].trigger('click')
      expect(wrapper.findComponent(TopMenuIndicatorControl).props('dark')).toBe(true)
      expect(wrapper.emitted('update:modelValue')).toHaveLength(1)
    }
    finally { wrapper.unmount() }
  })

  it.each(['normal', 'simple', 'bento', 'empty', 'immersive'])('%s 没有顶部菜单，不增加横条设置', async (layout) => {
    const wrapper = mount(AppearanceThemeEditor, { props: { modelValue: source(), layout }, global: { components } })
    try {
      if (layout !== 'empty') {
        await wrapper.find('.n-collapse-item__header-main').trigger('click')
      }
      expect(wrapper.findComponent(TopMenuIndicatorControl).exists()).toBe(false)
    }
    finally { wrapper.unmount() }
  })

  it('真实 HEX 输入修改横条，不改变自动配色或其它分组', async () => {
    const model = { ...source(), topMenu: { activeBarColor: '#FF7D00' } }
    const wrapper = mount(TopMenuIndicatorControl, {
      attachTo: document.body,
      props: { modelValue: model },
      global: { components },
    })
    try {
      await wrapper.find('.n-color-picker').trigger('click')
      await flushPromises()
      const input = new DOMWrapper(document.querySelector('.n-color-picker-panel input'))
      await input.setValue('#0E8F7E')
      await flushPromises()
      const next = wrapper.emitted('update:modelValue').at(-1)[0]
      expect(next.topMenu.activeBarColor).toBe('#0E8F7E')
      expect(next.navigationMode).toBe('auto')
      expect(next.header).toEqual(model.header)
      expect(next.topMenuDark).toEqual(model.topMenuDark)
    }
    finally { wrapper.unmount() }
  })
})
