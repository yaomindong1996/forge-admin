import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useBusinessWorkbenchStore } from '../businessWorkbenchStore'

describe('业务工作台菜单选中钉住', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('从弹出层跳转时保持新一级选中，不回落到旧页面', () => {
    const store = useBusinessWorkbenchStore()
    store.openMenu('module-b')
    store.prepareNavigation()
    store.onRouteChange()
    expect(store.activeMenu).toBeNull()
    expect(store.pinRoot).toBe('module-b')
    store.syncPin('module-a')
    expect(store.pinRoot).toBe('module-b')
    store.syncPin('module-b')
    expect(store.pinRoot).toBeNull()
  })

  it('点空白关闭会清掉钉住的一级选中', () => {
    const store = useBusinessWorkbenchStore()
    store.openMenu('module-b')
    store.closeMenus()
    expect(store.activeMenu).toBeNull()
    expect(store.pinRoot).toBeNull()
  })
})
