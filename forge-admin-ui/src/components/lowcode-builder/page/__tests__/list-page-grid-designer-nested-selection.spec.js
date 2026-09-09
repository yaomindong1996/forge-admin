import { shallowMount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import ListPageGridDesigner from '../ListPageGridDesigner.vue'

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
  it('opens the property panel for a child nested inside Tabs', async () => {
    const wrapper = shallowMount(ListPageGridDesigner, {
      props: {
        modelValue: createTabsLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'info_child',
      },
      global: {
        plugins: [createPinia()],
      },
    })

    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('提示面板')

    await wrapper.setProps({ activeBlockId: 'tabs_root' })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('Tabs 标签页')
  })
})
