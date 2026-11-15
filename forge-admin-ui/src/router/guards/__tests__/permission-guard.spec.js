import { describe, expect, it } from 'vitest'
import { canAccessRoute } from '../permission-guard'

describe('permission guard route access', () => {
  it('allows the notice list for authenticated users without a notice management menu', () => {
    expect(canAccessRoute(
      { path: '/system/notice-list' },
      { accessRoutes: [] },
    )).toBe(true)
  })

  it('allows published application portals without waiting for admin menus', () => {
    expect(canAccessRoute(
      { path: '/app/BuySale' },
      { accessRoutes: [] },
    )).toBe(true)
    expect(canAccessRoute(
      { path: '/app/hr-apply/extra' },
      { accessRoutes: [] },
    )).toBe(true)
  })

  it('continues to reject app-center management routes that are not in the user menu', () => {
    expect(canAccessRoute(
      { path: '/app-center' },
      { accessRoutes: [] },
    )).toBe(false)
  })

  it('continues to reject management routes that are not in the user menu', () => {
    expect(canAccessRoute(
      { path: '/message/manage' },
      { accessRoutes: [] },
    )).toBe(false)
  })

  it('allows the SSO bridge page without requiring it in accessRoutes', () => {
    expect(canAccessRoute(
      { path: '/report/design' },
      { accessRoutes: [] },
    )).toBe(true)
  })

  it('allows the iframe embed page without requiring it in accessRoutes', () => {
    expect(canAccessRoute(
      { path: '/iframe' },
      { accessRoutes: [] },
    )).toBe(true)
  })

  it('treats legacy print workspace deep links as the unified /print menu', () => {
    const menu = { accessRoutes: [{ path: '/print' }] }
    expect(canAccessRoute({ path: '/print/templates' }, menu)).toBe(true)
    expect(canAccessRoute({ path: '/print/sources' }, menu)).toBe(true)
    expect(canAccessRoute({ path: '/print/bindings' }, menu)).toBe(true)
    expect(canAccessRoute({ path: '/print/templates' }, { accessRoutes: [] })).toBe(false)
  })

  it('matches menu routes with :param segments without using RegExp', () => {
    const menu = { accessRoutes: [{ path: '/app-center/business-process/:processId' }] }
    expect(canAccessRoute({ path: '/app-center/business-process/abc' }, menu)).toBe(true)
    expect(canAccessRoute({ path: '/app-center/business-process' }, menu)).toBe(false)
    expect(canAccessRoute({ path: '/app-center/other/abc' }, menu)).toBe(false)
  })
})
