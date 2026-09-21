import { describe, expect, it } from 'vitest'
import { resolveBinding, resolveCollection } from '../binding'
import { validateFieldCatalog } from '../fieldCatalog'
import { formatValue } from '../formatters'
import { createPrintDocument } from '../types'

describe('authorized field binding', () => {
  const context = { main: { count: 0, active: false, empty: null }, children: { items: [{ name: 'A' }] } }
  it('preserves zero, false and null without walking prototypes', () => {
    for (const [field, value] of Object.entries(context.main)) {
      expect(resolveBinding({ source: 'FIELD', path: `main.${field}` }, context)).toBe(value)
    }
    expect(resolveBinding({ source: 'FIELD', path: 'main.missing' }, context)).toBeNull()
    expect(() => resolveBinding({ source: 'FIELD', path: 'main.constructor.name' }, context)).toThrow()
    expect(resolveBinding({ source: 'FIELD', path: 'main.inherited' }, { main: Object.create({ inherited: 123 }) })).toBeNull()
  })
  it('separates scalars from detail arrays and imposes a row limit', () => {
    expect(resolveCollection('children.items', context)).toEqual([{ name: 'A' }])
    expect(() => resolveBinding({ source: 'FIELD', path: 'children.items' }, context)).toThrow()
    expect(() => resolveCollection('main.count', context)).toThrow()
    expect(() => resolveCollection('children.items', { children: { items: Array.from({ length: 501 }, () => ({})) } })).toThrow()
  })
  it('checks every bound field against an exact authorized catalog', () => {
    const doc = createPrintDocument()
    doc.body = [{ id: 'a', kind: 'TEXT', binding: { source: 'FIELD', path: 'main.secret' } }]
    expect(validateFieldCatalog(doc, [{ path: 'main.count', type: 'NUMBER' }])[0].code).toBe('FIELD_NOT_ALLOWED')
    expect(validateFieldCatalog(doc, [{ path: 'main.secret', type: 'TEXT' }])).toEqual([])
  })
})

describe('non executable deterministic formatting', () => {
  it('retains zero and false with explicit empty/boolean labels', () => {
    expect(formatValue(0)).toBe('0')
    expect(formatValue(false)).toBe('false')
    expect(formatValue(false, { type: 'BOOLEAN', trueText: '是', falseText: '否' })).toBe('否')
    expect(formatValue(null, { emptyText: '—' })).toBe('—')
    expect(() => formatValue('false', { type: 'BOOLEAN' })).toThrow()
    expect(() => formatValue({ value: 'hidden' })).toThrow()
  })
  it('formats cents and decimal strings without losing 64 bit integer precision', () => {
    expect(formatValue('9223372036854775807', { type: 'MONEY' })).toBe('92233720368547758.07')
    expect(formatValue(-1, { type: 'MONEY' })).toBe('-0.01')
    expect(formatValue('1.005', { type: 'NUMBER', scale: 2 })).toBe('1.01')
    expect(formatValue('-1.005', { type: 'NUMBER', scale: 2 })).toBe('-1.01')
    expect(formatValue('00012.30', { type: 'NUMBER', scale: 3 })).toBe('12.300')
    expect(() => formatValue(9007199254740992, { type: 'MONEY' })).toThrow()
    expect(() => formatValue(1.2, { type: 'MONEY' })).toThrow()
    expect(() => formatValue('1e10', { type: 'NUMBER' })).toThrow()
    expect(() => formatValue(9007199254740992)).toThrow()
  })
  it('formats server-local dates without changing the timezone or accepting impossible dates', () => {
    expect(formatValue('2026-09-18T12:30:40', { type: 'DATE', datePattern: 'YYYY-MM-DD HH:mm' })).toBe('2026-09-18 12:30')
    expect(() => formatValue('2026-02-30', { type: 'DATE' })).toThrow()
    expect(formatValue('<script>alert(1)</script>')).toBe('<script>alert(1)</script>')
    expect(formatValue(100, { type: 'MONEY_UPPER' })).toBe('壹元整')
  })
})
