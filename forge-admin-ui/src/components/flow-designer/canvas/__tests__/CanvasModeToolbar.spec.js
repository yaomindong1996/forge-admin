import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CanvasModeToolbar from '../CanvasModeToolbar.vue'

describe('CanvasModeToolbar', () => {
  it('底部工具条含添加与连线入口', () => {
    const wrapper = mount(CanvasModeToolbar, {
      props: { tool: 'select' },
      global: {
        stubs: {
          NPopover: {
            template: '<div class="n-popover-stub"><slot name="trigger" /><slot /></div>',
          },
        },
      },
    })
    const labels = wrapper.findAll('button').map(b => b.attributes('aria-label'))
    expect(labels).toContain('添加节点')
    expect(labels).toContain('连线')
    expect(labels).toContain('选择')
    expect(wrapper.find('.canvas-mode-toolbar').exists()).toBe(true)
  })
})
