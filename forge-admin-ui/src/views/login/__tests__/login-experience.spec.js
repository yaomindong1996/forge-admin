import { mount } from '@vue/test-utils'
import { NImage, NModal } from 'naive-ui'
import { afterEach, describe, expect, it } from 'vitest'
import CommunitySupportDialog from '@/components/common/CommunitySupportDialog.vue'
import LoginBrandPanel from '../components/LoginBrandPanel.vue'

const wrappers = []
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
})

describe('login visual and support contracts', () => {
  it('has three independent illustrations with accessible carousel controls and real branding', async () => {
    const wrapper = mount(LoginBrandPanel, {
      props: { logo: '/tenant-logo.png', systemName: '测试工作区' },
      global: { stubs: { NCarousel: { template: '<div><slot /><slot name="dots" :current-index="0" :to="() => {}" /></div>' } } },
    })
    wrappers.push(wrapper)
    expect(wrapper.find('.brand-lockup img').attributes('src')).toBe('/tenant-logo.png')
    expect(wrapper.text()).toContain('测试工作区')
    const images = wrapper.findAll('.brand-artwork')
    // Naive 轮播会克隆首尾节点；检查独立资产数而非内部节点数。
    expect(images.length).toBeGreaterThanOrEqual(3)
    expect(new Set(images.map(image => image.attributes('src'))).size).toBe(3)
    expect(wrapper.findAll('.brand-dots button').every(button => !!button.attributes('aria-label'))).toBe(true)
    await wrapper.find('.brand-lockup img').trigger('error')
    expect(wrapper.emitted('logoError')).toHaveLength(1)
  })

  it('uses configured group QR and retains the two existing support QRs without extra requests', async () => {
    const wrapper = mount(CommunitySupportDialog, {
      props: { show: true, groupImage: '/configured-qr.png', groupName: '业务交流群' },
      global: { stubs: { NModal: { template: '<div><slot /><slot name="footer" /></div>' } } },
    })
    wrappers.push(wrapper)
    expect(wrapper.findAllComponents(NImage)).toHaveLength(3)
    expect(wrapper.findAllComponents(NImage)[0].props('src')).toBe('/configured-qr.png')
    expect(document.body.textContent).toContain('业务交流群')
    document.querySelector('.support-footer button').click()
    expect(wrapper.emitted('update:show')).toEqual([[false]])
    await wrapper.setProps({ groupImage: '' })
    expect(wrapper.findAllComponents(NImage)[0].props('src')).toContain('forge-wechat-group.png')
    expect(document.body.textContent).toContain('联系维护者')
  })

  it('forwards the library modal close event', () => {
    const wrapper = mount(CommunitySupportDialog, { props: { show: false } })
    wrappers.push(wrapper)
    wrapper.findComponent(NModal).vm.$emit('update:show', false)
    expect(wrapper.emitted('update:show')).toEqual([[false]])
  })
})
