import { mount } from '@vue/test-utils'
import { NCarousel, NImage, NModal } from 'naive-ui'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import CommunitySupportDialog from '@/components/common/CommunitySupportDialog.vue'
import LoginBrandPanel from '../components/LoginBrandPanel.vue'

const wrappers = []
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.useRealTimers()
})

describe('login visual and support contracts', () => {
  it('has three independent illustrations with accessible carousel controls and real branding', async () => {
    const wrapper = mount(LoginBrandPanel, {
      props: { logo: '/tenant-logo.png', systemName: '测试工作区' },
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

  it('keeps visible controls outside the clipped carousel and synchronizes every navigation path', async () => {
    const wrapper = mount(LoginBrandPanel, { props: { logo: '/tenant-logo.png', systemName: '测试工作区' } })
    wrappers.push(wrapper)
    const carousel = wrapper.findComponent(NCarousel)
    expect(wrapper.find('.brand-carousel .brand-controls').exists()).toBe(false)
    expect(wrapper.find('.brand-controls').exists()).toBe(true)
    expect(carousel.props('showDots')).toBe(false)

    await wrapper.findAll('.brand-dots button')[2].trigger('click')
    expect(carousel.props('currentIndex')).toBe(2)
    await wrapper.find('[aria-label="下一张轮播图"]').trigger('click')
    expect(carousel.props('currentIndex')).toBe(0)
    await wrapper.find('[aria-label="上一张轮播图"]').trigger('click')
    expect(carousel.props('currentIndex')).toBe(2)

    carousel.vm.$emit('update:currentIndex', 1)
    await nextTick()
    expect(wrapper.find('.brand-dots [aria-current="true"]').attributes('aria-label')).toBe('业务搭建，轻松起步')
    expect(carousel.props('currentIndex')).toBe(1)
  })

  it('updates the external indicator during real library autoplay', async () => {
    vi.useFakeTimers()
    const wrapper = mount(LoginBrandPanel, { props: { logo: '/tenant-logo.png', systemName: '测试工作区' } })
    wrappers.push(wrapper)
    await nextTick()
    expect(wrapper.findComponent(NCarousel).props('autoplay')).toBe(true)
    await vi.advanceTimersByTimeAsync(6000)
    expect(wrapper.findComponent(NCarousel).props('currentIndex')).toBe(1)
    expect(wrapper.find('.brand-dots [aria-current="true"]').attributes('aria-label')).toBe('业务搭建，轻松起步')
  })

  it('forwards the library modal close event', () => {
    const wrapper = mount(CommunitySupportDialog, { props: { show: false } })
    wrappers.push(wrapper)
    wrapper.findComponent(NModal).vm.$emit('update:show', false)
    expect(wrapper.emitted('update:show')).toEqual([[false]])
  })
})
