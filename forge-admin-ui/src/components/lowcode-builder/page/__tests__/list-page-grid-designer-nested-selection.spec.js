import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { useListDesignerStore } from '@/store'
import ListPageGridDesigner from '../ListPageGridDesigner.vue'

vi.mock('@/router', () => ({ default: { currentRoute: { value: { query: {} } } } }))

function createTabsLayout() {
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'simple-crud',
    items: [
      {
        id: 'tabs_root',
        blockType: 'tabs',
        label: 'Tabs 标签页',
        gridX: 0,
        gridY: 0,
        gridW: 12,
        gridH: 6,
        props: {
          tabs: [
            {
              key: 'tab1',
              title: '标签一',
              children: [
                {
                  id: 'info_child',
                  blockType: 'info-panel',
                  label: 'Tabs 内提示',
                  gridX: 0,
                  gridY: 0,
                  gridW: 12,
                  gridH: 2,
                  props: {
                    title: '提示信息',
                    content: '嵌套内容',
                    type: 'info',
                  },
                },
              ],
            },
          ],
        },
      },
    ],
  }
}

describe('list page grid designer nested selection', () => {
  it('invalid and removed selections leave an empty panel without reading a null block', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: { modelValue: createTabsLayout(), panelOnly: true, activeBlockId: 'missing' },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.property-empty').exists()).toBe(true)
    await wrapper.setProps({ activeBlockId: 'info_child' })
    expect(wrapper.find('.prop-title').text()).toBe('提示面板')
    await wrapper.setProps({ modelValue: { ...createTabsLayout(), items: [] } })
    await nextTick()
    expect(wrapper.find('.property-empty').exists()).toBe(true)
    wrapper.unmount()
  })

  it('opens the property panel for a child nested inside Tabs', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createTabsLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'info_child',
      },
      global: {
        plugins: [createPinia()],
        // 只验证真实选择/属性面板；数据绑定编辑器的消息 Provider 和请求有独立用例。
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })

    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('提示面板')

    // 首次选择之后，布局更新不能反复用旧 activeBlockId 覆盖用户当前选择。
    useListDesignerStore().selectedBlockId = 'tabs_root'
    await wrapper.setProps({ modelValue: { ...createTabsLayout(), gap: 10 } })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('Tabs 标签页')

    await wrapper.setProps({ activeBlockId: 'tabs_root' })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('Tabs 标签页')
    wrapper.unmount()
  })
})
