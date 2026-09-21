import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { registerBuiltInPrintComponents } from '../businessPrintComponents'
import { addSection } from '../elementCatalog'
import { insertRegisteredPrintComponent, PrintComponentRegistry } from '../printComponentRegistry'

function textFragment(extra = {}) {
  return {
    kind: 'ELEMENT',
    element: {
      id: 'component-text',
      type: 'TEXT',
      xMm: 0,
      yMm: 0,
      widthMm: 50,
      heightMm: 10,
      binding: { source: 'CONSTANT', value: '业务组件' },
      ...extra,
    },
  }
}

describe('trusted print component registry', () => {
  let store
  beforeEach(() => {
    setActivePinia(createPinia())
    store = usePrintDesignerStore()
    store.load(createPrintDocument(), [{ path: 'main.status', label: '审批状态', type: 'TEXT' }])
  })

  it('rejects invalid definitions, duplicates and declared async factories', () => {
    const registry = new PrintComponentRegistry()
    expect(() => registry.register({ key: 'bad key', label: '错误', icon: 'custom', factory: () => textFragment() })).toThrow()
    expect(() => registry.register({ key: 'app.valid', label: '有效', icon: 'custom', factory: async () => textFragment() })).toThrow(/同步工厂/)
    registry.register({ key: 'app.valid', label: '有效', icon: 'custom', factory: () => textFragment() })
    expect(() => registry.register({ key: 'app.valid', label: '重复', icon: 'custom', factory: () => textFragment() })).toThrow(/已注册/)
    expect(registry.list()).toEqual([{ key: 'app.valid', label: '有效', icon: 'custom' }])
  })

  it('atomically rejects promises, executable values and invalid protocol fragments', () => {
    const before = store.serialize()
    const cases = [
      { key: 'app.promise', factory: () => Promise.resolve(textFragment()) },
      { key: 'app.function', factory: () => textFragment({ formatter: () => 'unsafe' }) },
      { key: 'app.script', factory: () => textFragment({ type: 'SCRIPT' }) },
    ]
    for (const item of cases) {
      const registry = new PrintComponentRegistry()
      registry.register({ ...item, label: item.key, icon: 'custom' })
      expect(insertRegisteredPrintComponent(store, item.key, undefined, registry)).toBe(false)
      expect(store.serialize()).toBe(before)
    }
  })

  it('inserts standard fragments with regenerated IDs and exports only protocol JSON', () => {
    const registry = new PrintComponentRegistry()
    registry.register({ key: 'app.text', label: '业务文本', icon: 'custom', factory: () => textFragment() })
    addSection(store, 'FIXED')
    expect(insertRegisteredPrintComponent(store, 'app.text', undefined, registry)).toBe(true)
    expect(store.activeElement).toMatchObject({ type: 'TEXT', binding: { source: 'CONSTANT', value: '业务组件' } })
    expect(store.activeElement.id).not.toBe('component-text')
    const json = store.serialize()
    expect(json).not.toContain('app.text')
    expect(json).not.toContain('factory')
    expect(json).not.toContain('registry')
  })

  it('provides three built-ins and binds approved fields when available', () => {
    const registry = registerBuiltInPrintComponents(new PrintComponentRegistry())
    expect(registry.list().map(item => item.key)).toEqual(['forge.approval-status', 'forge.signature-position', 'forge.contract-terms'])
    expect(insertRegisteredPrintComponent(store, 'forge.approval-status', undefined, registry)).toBe(true)
    expect(store.activeSurface.kind).toBe('FIXED')
    expect(store.selectedIds).toHaveLength(2)
    expect(store.activeSurface.elements.some(item => item.binding?.path === 'main.status')).toBe(true)
    expect(insertRegisteredPrintComponent(store, 'forge.contract-terms', undefined, registry)).toBe(true)
    expect(store.activeElement).toMatchObject({ type: 'TEXT', binding: { source: 'CONSTANT' } })
  })
})
