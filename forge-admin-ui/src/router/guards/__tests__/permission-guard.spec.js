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
})
