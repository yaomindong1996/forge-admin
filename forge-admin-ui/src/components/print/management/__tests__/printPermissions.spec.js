import { describe, expect, it } from 'vitest'
import { hasPrintPermission } from '../printPermissions'

describe('print permissions', () => {
  it('allows platform super administrators without a legacy dataPermission array', () => {
    expect(hasPrintPermission({ isAdmin: true, permissions: [] }, 'print:template:view')).toBe(true)
  })

  it('accepts current and compatibility wildcard permissions', () => {
    expect(hasPrintPermission({ permissions: ['*:*:*'] }, 'print:template:manage')).toBe(true)
    expect(hasPrintPermission({ getDataPermission: ['**'] }, 'print:template:publish')).toBe(true)
  })

  it('accepts exact permissions from either platform field', () => {
    expect(hasPrintPermission({ permissions: ['print:template:view'] }, 'print:template:view')).toBe(true)
    expect(hasPrintPermission({ getDataPermission: ['print:template:manage'] }, 'print:template:manage')).toBe(true)
  })

  it('rejects empty, unrelated and missing permission input', () => {
    expect(hasPrintPermission({ permissions: [] }, 'print:template:view')).toBe(false)
    expect(hasPrintPermission({ permissions: ['system:user:list'] }, 'print:template:view')).toBe(false)
    expect(hasPrintPermission({ permissions: ['*:*:*'] }, '')).toBe(false)
    expect(hasPrintPermission(null, 'print:template:view')).toBe(false)
  })
})
