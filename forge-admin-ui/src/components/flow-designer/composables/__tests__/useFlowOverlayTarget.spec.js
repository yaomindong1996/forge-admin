import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { useFlowOverlayTarget } from '../useFlowOverlayTarget.js'

describe('全屏浮层挂载位置', () => {
  it('进入全屏跟随目标，退出后恢复默认 body 传送', async () => {
    const descriptor = Object.getOwnPropertyDescriptor(document, 'fullscreenElement')
    let fullscreenElement = null
    Object.defineProperty(document, 'fullscreenElement', { configurable: true, get: () => fullscreenElement })
    const wrapper = mount({ setup: () => ({ target: useFlowOverlayTarget() }), template: '<div />' })
    try {
      expect(wrapper.vm.target).toBeNull()
      fullscreenElement = document.createElement('section')
      document.dispatchEvent(new Event('fullscreenchange'))
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.target).toBe(fullscreenElement)
      fullscreenElement = null
      document.dispatchEvent(new Event('fullscreenchange'))
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.target).toBeNull()
    }
    finally {
      wrapper.unmount()
      if (descriptor)
        Object.defineProperty(document, 'fullscreenElement', descriptor)
      else
        Reflect.deleteProperty(document, 'fullscreenElement')
    }
  })
})
