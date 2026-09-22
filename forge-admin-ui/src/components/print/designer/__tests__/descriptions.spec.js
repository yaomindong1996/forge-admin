import { describe, expect, it } from 'vitest'
import { assertPrintDocument } from '../../protocol/validate'
import { createFormDetailDocument, descriptionFields, descriptionItemsFromCatalog } from '../descriptions'

describe('form detail descriptions', () => {
  it('fills main fields and skips detail collection columns', () => {
    const items = descriptionItemsFromCatalog([
      { path: 'main.name', label: '名称', type: 'TEXT' },
      { path: 'items', label: '明细', type: 'COLLECTION' },
      { path: 'items.qty', label: '数量', type: 'NUMBER' },
      { path: 'flow.status', label: '状态', type: 'TEXT' },
    ])
    expect(descriptionFields([
      { path: 'main.name', type: 'TEXT' },
      { path: 'items', type: 'COLLECTION' },
      { path: 'items.qty', type: 'NUMBER' },
    ]).map(field => field.path)).toEqual(['main.name'])
    expect(items.map(item => item.binding.path)).toEqual(['main.name', 'flow.status'])
    expect(assertPrintDocument(createFormDetailDocument([
      { path: 'main.name', label: '名称', type: 'TEXT' },
    ])).body[0].elements[0].descriptions.items[0].label).toBe('名称')
  })
})
