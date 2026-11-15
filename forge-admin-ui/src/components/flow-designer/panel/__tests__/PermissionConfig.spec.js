import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { NSwitch } from 'naive-ui'
import PermissionConfig from '../PermissionConfig.vue'

describe('PermissionConfig', () => {
  it('切换开关会向父级 emit 对应权限字段', async () => {
    const onUpdate = vi.fn()
    const wrapper = mount(PermissionConfig, {
      props: {
        config: {
          allowApprove: true,
          allowReject: true,
          allowRejectToStart: false,
          allowDelegate: true,
          allowTerminate: false,
          requireSignature: false,
          requireComment: true,
        },
        'onUpdate:config': onUpdate,
      },
      global: { components: { NSwitch } },
    })

    const switches = wrapper.findAllComponents(NSwitch)
    const byLabel = Object.fromEntries(
      switches.map(sw => [sw.attributes('aria-label'), sw]),
    )

    await byLabel['强制签名'].vm.$emit('update:value', true)
    await byLabel['退回发起人修改'].vm.$emit('update:value', true)
    await byLabel['允许终止'].vm.$emit('update:value', true)

    expect(onUpdate).toHaveBeenCalledWith({ requireSignature: true })
    expect(onUpdate).toHaveBeenCalledWith({ allowRejectToStart: true })
    expect(onUpdate).toHaveBeenCalledWith({ allowTerminate: true })
  })
})
