import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useUserStore } from '@/store/modules/user'
import AccountIdentity from '../AccountIdentity.vue'

const resolveUrl = vi.hoisted(() => vi.fn())
vi.mock('@/utils/file', () => ({ resolveRenderableFileUrl: resolveUrl }))
vi.mock('@/store', async () => ({ useUserStore: (await import('@/store/modules/user')).useUserStore }))
const options = { global: { stubs: { NAvatar: {
  name: 'NAvatar',
  props: ['src'],
  emits: ['error'],
  template: '<span class="avatar" :data-src="src"><slot /></span>',
} } } }

describe('账户身份展示', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    resolveUrl.mockReset()
  })

  it('读取真实用户姓名，空头像显示首字，不增加可点击的二级入口', () => {
    useUserStore().userInfo = { realName: '预览用户' }
    const wrapper = mount(AccountIdentity, options)
    try {
      expect(wrapper.find('.account-name').text()).toBe('预览用户')
      expect(wrapper.find('.avatar').text()).toBe('预')
      expect(wrapper.find('button').exists()).toBe(false)
      expect(resolveUrl).not.toHaveBeenCalled()
    }
    finally { wrapper.unmount() }
  })

  it('旧身份头像迟到不覆盖新身份，使用共用文件解析器', async () => {
    const pending = {}
    resolveUrl.mockImplementation(id => new Promise((resolve) => {
      pending[id] = resolve
    }))
    const store = useUserStore()
    store.userInfo = { realName: '原用户', avatar: 'old-file' }
    const wrapper = mount(AccountIdentity, options)
    try {
      store.userInfo = { realName: '新用户', avatar: 'new-file' }
      await flushPromises()
      pending['new-file']('blob:new')
      await flushPromises()
      pending['old-file']('blob:old')
      await flushPromises()
      expect(wrapper.find('.avatar').attributes('data-src')).toBe('blob:new')
      expect(wrapper.find('.account-name').text()).toBe('新用户')
      expect(resolveUrl).toHaveBeenCalledWith('new-file', undefined, true)
    }
    finally { wrapper.unmount() }
  })

  it('图片失败回退姓名，不重新请求形成循环', async () => {
    resolveUrl.mockResolvedValue('blob:avatar')
    useUserStore().userInfo = { username: 'preview', avatar: 'file' }
    const wrapper = mount(AccountIdentity, options)
    try {
      await flushPromises()
      wrapper.findComponent({ name: 'NAvatar' }).vm.$emit('error')
      await flushPromises()
      expect(wrapper.find('.avatar').text()).toBe('p')
      expect(resolveUrl).toHaveBeenCalledOnce()
    }
    finally { wrapper.unmount() }
  })
})
