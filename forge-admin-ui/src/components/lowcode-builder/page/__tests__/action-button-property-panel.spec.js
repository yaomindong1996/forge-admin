import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import ListPageGridDesigner from '../ListPageGridDesigner.vue'

vi.mock('@/router', () => ({ default: { currentRoute: { value: { query: {} } } } }))

function createActionButtonLayout() {
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'custom',
    items: [
      {
        id: 'btn_apply',
        blockType: 'action-button',
        label: '按钮',
        gridX: 0,
        gridY: 0,
        gridW: 4,
        gridH: 1,
        props: {
          text: '发起请假',
          type: 'primary',
          size: 'medium',
          block: false,
          events: [{
            id: 'evt_1',
            trigger: 'click',
            action: 'navigate',
            targetPageKey: 'page_leave_apply',
            params: [],
          }],
        },
      },
    ],
  }
}

describe('action-button property panel', () => {
  it('shows style and click-action fields when an action-button is selected', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createActionButtonLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'btn_apply',
      },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toMatch(/按钮/)
    const html = wrapper.find('.property-body').html()
    expect(html).toContain('基础样式')
    expect(html).toContain('点击动作')
    expect(html).toContain('按钮状态')
    expect(html).toContain('style-grid')
    expect(html).toContain('action-button-event-grid')
    // 文案应回填到基础样式输入框；空值即「属性面板没东西」观感
    expect(html).toContain('发起请假')
    wrapper.unmount()
  })
})
