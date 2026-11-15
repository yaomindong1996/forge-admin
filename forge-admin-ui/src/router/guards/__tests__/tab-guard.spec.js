import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createTabGuard } from '../tab-guard'

const tabStore = {
  tabs: [],
  dirtyNavigationBypassPath: '',
  addTab: vi.fn((tab) => {
    tabStore.tabs.push(tab)
  }),
  updateTabMeta: vi.fn((key, patch) => {
    const tab = tabStore.tabs.find(item => item.key === key || item.path === key)
    if (tab)
      Object.assign(tab, patch)
  }),
  updateTabTitle: vi.fn(),
  setActiveTab: vi.fn(),
  removeTabSilently: vi.fn(),
  authorizeDirtyNavigation: vi.fn(),
}

vi.mock('@/store', () => ({
  useTabStore: () => tabStore,
  usePermissionStore: () => ({ allMenus: [] }),
}))

vi.mock('@/utils/tab-interactions', () => ({
  confirmDirtyTabs: vi.fn(async () => true),
}))

describe('createTabGuard', () => {
  let afterEach

  beforeEach(() => {
    tabStore.tabs = []
    tabStore.addTab.mockClear()
    tabStore.updateTabMeta.mockClear()
    tabStore.setActiveTab.mockClear()
    const router = {
      beforeEach: vi.fn(),
      afterEach: vi.fn((fn) => {
        afterEach = fn
      }),
    }
    createTabGuard(router)
  })

  it('我发起的带不同 title 参数复用同一页签并刷新 path', async () => {
    await afterEach({
      name: 'FlowStarted',
      path: '/flow/started',
      fullPath: '/flow/started?title=A',
      meta: { title: '我发起的' },
      matched: [{ path: '/flow/started' }],
      query: { title: 'A' },
    })
    expect(tabStore.addTab).toHaveBeenCalledWith(expect.objectContaining({
      key: '/flow/started',
      path: '/flow/started?title=A',
      title: '我发起的',
    }))

    await afterEach({
      name: 'FlowStarted',
      path: '/flow/started',
      fullPath: '/flow/started?title=B',
      meta: { title: '我发起的' },
      matched: [{ path: '/flow/started' }],
      query: { title: 'B' },
    })
    expect(tabStore.addTab).toHaveBeenCalledTimes(1)
    expect(tabStore.updateTabMeta).toHaveBeenCalledWith('/flow/started', expect.objectContaining({
      path: '/flow/started?title=B',
    }))
    expect(tabStore.setActiveTab).toHaveBeenLastCalledWith('/flow/started')
  })
})
