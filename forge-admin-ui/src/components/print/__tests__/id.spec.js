import { describe, expect, it, vi } from 'vitest'
import { createPrintRandomToken, newPrintId, newPrintTemplateCode } from '../id'

describe('print id helpers', () => {
  it('prefers crypto.randomUUID when available', () => {
    vi.stubGlobal('crypto', {
      randomUUID: () => '123e4567-e89b-12d3-a456-426614174000',
    })
    expect(createPrintRandomToken()).toBe('123e4567e89b12d3a456426614174000')
    expect(newPrintId()).toBe('p_123e4567e89b12d3a456426614174000')
    expect(newPrintTemplateCode()).toBe('print_123e4567e89b12d3a456426614174000')
    vi.unstubAllGlobals()
  })

  it('falls back when randomUUID is missing', () => {
    vi.stubGlobal('crypto', {
      getRandomValues(bytes) {
        bytes.fill(7)
        return bytes
      },
    })
    expect(createPrintRandomToken()).toBe('07070707070707070707070707070707')
    expect(newPrintId().startsWith('p_')).toBe(true)
    expect(newPrintTemplateCode().startsWith('print_')).toBe(true)
    vi.unstubAllGlobals()
  })

  it('does not throw when crypto has no randomUUID', () => {
    vi.stubGlobal('crypto', {})
    expect(() => newPrintTemplateCode()).not.toThrow()
    expect(newPrintTemplateCode().startsWith('print_')).toBe(true)
    vi.unstubAllGlobals()
  })
})
