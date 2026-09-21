import { describe, expect, it } from 'vitest'
import {
  describeFieldPath,
  fieldGroupKey,
  groupPrintFields,
  groupedFieldSelectOptions,
} from '../fieldGroups'

const catalog = [
  { path: 'main.number', label: '单号', type: 'TEXT' },
  { path: 'main.total', label: '合计', type: 'MONEY' },
  { path: 'children.items', label: '采购明细', type: 'COLLECTION' },
  { path: 'children.items.name', label: '物料', type: 'TEXT' },
  { path: 'children.items.amount', label: '金额', type: 'MONEY' },
  { path: 'children.payments', label: '付款计划', type: 'COLLECTION' },
  { path: 'children.payments.planDate', label: '计划日', type: 'DATE' },
  { path: 'flow.status', label: '审批状态', type: 'TEXT' },
]

describe('fieldGroups', () => {
  it('groups catalog into 主表 / 子表 / 流程', () => {
    const groups = groupPrintFields(catalog)
    expect(groups.map(group => group.key)).toEqual([
      'main',
      'children.items',
      'children.payments',
      'flow',
    ])
    expect(groups[0]).toMatchObject({ kind: 'main', title: '主表' })
    expect(groups[1]).toMatchObject({ kind: 'collection', title: '子表 · 采购明细' })
    expect(groups[1].fields.some(field => field.type === 'COLLECTION')).toBe(true)
    expect(groups[3].title).toBe('流程')
  })

  it('builds grouped select options without detail columns by default', () => {
    const options = groupedFieldSelectOptions(catalog)
    expect(options.map(group => group.label)).toEqual(['主表', '流程'])
    expect(options[0].children.map(item => item.value)).toEqual(['main.number', 'main.total'])
  })

  it('lists relative child fields under a collection', () => {
    const options = groupedFieldSelectOptions(catalog, { onlyUnder: 'children.items' })
    expect(options[0].children.map(item => item.value)).toEqual(['name', 'amount'])
  })

  it('describes a path with group title', () => {
    expect(describeFieldPath(catalog, 'main.number')).toBe('主表 / 单号')
    expect(describeFieldPath(catalog, 'children.items.name')).toBe('子表 · 采购明细 / 物料')
    expect(fieldGroupKey('children.items.amount', ['children.items'])).toBe('children.items')
  })
})
