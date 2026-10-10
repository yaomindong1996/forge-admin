import { flushPromises, mount } from '@vue/test-utils'
import { NButton } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getSystemVersion } from '@/api/system/version'
import { useSystemVersionStore } from '@/stores/system/versionStore'
import { copyText } from '@/utils/tab-interactions'
import SystemAboutModal from '../SystemAboutModal.vue'

vi.mock('@/api/system/version', () => ({ getSystemVersion: vi.fn() }))
vi.mock('@/utils/tab-interactions', () => ({ copyText: vi.fn() }))
vi.mock('@/components/DictTag.vue', () => ({ default: { props: ['value'], template: '<span>{{ value }}</span>' } }))

describe('关于系统 Naive 弹窗', () => {
  let wrapper
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('__FORGE_BUILD_INFO__', {
      version: '1.2.0',
      client: 'admin-ui',
      builtAt: '2026-10-10T10:00:00Z',
      notes: '## [1.2.0]\n<img src=x onerror="alert(1)">',
    })
  })
  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
  })

  it('真实组件渲染信息、纯文本说明、复制和关闭', async () => {
    getSystemVersion.mockResolvedValue({ code: 200, data: {
      version: '1.2.0',
      coreVersion: '1.2.0',
      edition: 'community',
      build: { service: 'forge-admin-server', time: '2026-10-10T10:00:00Z', commit: null },
    } })
    const store = useSystemVersionStore()
    await store.open()
    wrapper = mount(SystemAboutModal, { attachTo: document.body })
    await flushPromises()
    expect(document.body.textContent).toContain('当前系统')
    expect(document.body.textContent).toContain('v1.2.0')
    expect(document.body.textContent).toContain('forge-admin-server')
    expect(document.querySelectorAll('.version-section')).toHaveLength(2)
    expect(document.querySelector('.version-emblem svg').getAttribute('stroke')).toBe('currentColor')
    expect(document.querySelector('.version-heading__motif').getAttribute('aria-hidden')).toBe('true')
    document.querySelector('.n-collapse-item__header-main').click()
    await flushPromises()
    expect(document.querySelector('pre').textContent).toContain('<img src=x')
    expect(document.querySelector('.version-notes img')).toBeNull()
    const copy = wrapper.findAllComponents(NButton).find(button => button.text() === '复制版本信息')
    await copy.trigger('click')
    expect(copyText).toHaveBeenCalledWith(expect.stringContaining('后端运行信息\n发行版本：1.2.0'), '版本信息已复制')
    document.querySelector('.n-base-close').click()
    await flushPromises()
    expect(store.visible).toBe(false)
  })

  it('接口失败保留前端信息与刷新操作，不显示虚假的后端版本', async () => {
    getSystemVersion.mockRejectedValue(new Error('404'))
    await useSystemVersionStore().open()
    wrapper = mount(SystemAboutModal, { attachTo: document.body })
    await flushPromises()
    expect(document.body.textContent).toContain('暂时无法获取后端版本')
    expect(document.body.textContent).toContain('版本待确认')
    expect(document.body.textContent).toContain('当前前端')
    const refresh = wrapper.findAllComponents(NButton).find(button => button.text() === '刷新信息')
    await refresh.trigger('click')
    await flushPromises()
    expect(getSystemVersion).toHaveBeenCalledTimes(2)
  })

  it('装饰与分组不掩盖前后端版本不一致提示', async () => {
    getSystemVersion.mockResolvedValue({ code: 200, data: { version: '1.1.0', coreVersion: '1.1.0' } })
    await useSystemVersionStore().open()
    wrapper = mount(SystemAboutModal, { attachTo: document.body })
    await flushPromises()
    expect(document.querySelector('.n-alert').textContent).toContain('前后端版本不一致')
    expect(document.querySelector('.version-heading').textContent).toContain('v1.1.0')
    const sections = document.querySelectorAll('.version-section')
    expect(sections[0].textContent).toContain('1.1.0')
    expect(sections[1].textContent).toContain('1.2.0')
  })
})
