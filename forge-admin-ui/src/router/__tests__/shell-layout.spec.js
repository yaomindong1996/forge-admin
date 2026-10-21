import { beforeEach, describe, expect, it } from 'vitest'
import {
  isApplicationRuntimePath,
  isEmptyShellPath,
  pendingShellLayout,
  resolvePendingShellLayout,
  resolveShellLayoutName,
} from '../shell-layout'

describe('shell-layout', () => {
  beforeEach(() => {
    pendingShellLayout.value = null
  })

  it('treats application design runtime as empty shell', () => {
    expect(isEmptyShellPath('/app-center/application/BuySale/runtime')).toBe(true)
    expect(isApplicationRuntimePath('/app-center/application/BuySale/runtime')).toBe(true)
    expect(isEmptyShellPath('/app-center/application/BuySale')).toBe(true)
    expect(isEmptyShellPath('/app-center')).toBe(false)
  })

  it('locks empty shell from pending navigation before route meta settles', () => {
    expect(resolvePendingShellLayout({
      path: '/app-center/application/BuySale/runtime',
      meta: { layout: 'empty' },
    })).toBe('empty')

    expect(resolveShellLayoutName({
      routeMetaLayout: undefined,
      routePath: '/',
      locationPath: '/app-center/application/BuySale/runtime',
      pending: 'empty',
      appLayout: 'side-flyout',
    })).toBe('empty')

    expect(resolveShellLayoutName({
      routeMetaLayout: undefined,
      routePath: '/',
      locationPath: '/app-center/application/BuySale/runtime',
      pending: null,
      appLayout: 'side-flyout',
    })).toBe('empty')
  })

  it('keeps system layout for app-center list', () => {
    expect(resolveShellLayoutName({
      routeMetaLayout: undefined,
      routePath: '/app-center',
      locationPath: '/app-center',
      pending: null,
      appLayout: 'side-flyout',
    })).toBe('side-flyout')
  })
})
