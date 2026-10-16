import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PageWidgetRenderer from '../PageWidgetRenderer.vue'

vi.mock('@/utils', () => ({ request: vi.fn() }))

describe('PageWidgetRenderer countdown', () => {
  it('does not pass invalid separator prop to n-countdown', () => {
    const warnings = []
    const wrapper = mount(PageWidgetRenderer, {
      props: {
        componentKey: 'countdown',
        propsData: {
          title: '倒计时',
          duration: 3600000,
          active: true,
          precision: 0,
          separator: '-',
        },
      },
      global: {
        stubs: {
          NCountdown: {
            name: 'NCountdown',
            props: {
              duration: Number,
              active: Boolean,
              precision: Number,
              render: Function,
              separator: String,
            },
            template: '<span class="n-countdown-stub" />',
          },
        },
        config: {
          warnHandler(msg) {
            warnings.push(String(msg || ''))
          },
        },
      },
    })

    const countdown = wrapper.findComponent({ name: 'NCountdown' })
    expect(countdown.exists()).toBe(true)
    expect(countdown.props('separator')).toBeUndefined()
    expect(typeof countdown.props('render')).toBe('function')
    expect(countdown.props('render')({
      hours: 1,
      minutes: 2,
      seconds: 3,
      milliseconds: 0,
    })).toBe('01-02-03')
    expect(warnings.some(msg => msg.includes('Extraneous non-props attributes') && msg.includes('separator'))).toBe(false)
    wrapper.unmount()
  })
})
