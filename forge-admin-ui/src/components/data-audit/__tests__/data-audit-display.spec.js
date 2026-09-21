import { describe, expect, it } from 'vitest'
import {
  actorInitial,
  auditDiffHeadings,
  auditFieldSummary,
  auditFieldTitle,
  groupAuditFields,
  isTechnicalAuditCode,
  resolveAuditValueView,
  sortAuditFields,
  visibleAuditFields,
} from '../data-audit-display'

describe('data-audit-display', () => {
  const fields = [
    { id: 'child', fieldLabel: '明细数量', relationKey: 'items', sortOrder: 1 },
    { id: 'main-late', fieldLabel: '备注', sortOrder: 2 },
    { id: 'main-first', fieldLabel: '名称', sortOrder: 1 },
    { id: 'summary', fieldType: 'CHILD_SUMMARY', fieldCode: '__childRows', sortOrder: 0 },
    { id: 'main-last', fieldLabel: '状态', sortOrder: 3 },
  ]

  it('sorts main table fields before child changes', () => {
    expect(sortAuditFields(fields).map(item => item.id)).toEqual([
      'main-first',
      'main-late',
      'main-last',
      'summary',
      'child',
    ])
  })

  it('shows a compact preview before expanding all changes', () => {
    expect(visibleAuditFields(fields).map(item => item.id)).toEqual([
      'main-first',
      'main-late',
      'main-last',
      'summary',
    ])
    expect(visibleAuditFields(fields, true)).toHaveLength(5)
  })

  it('uses operation-specific before and after headings', () => {
    expect(auditDiffHeadings('UPDATE')).toEqual({ before: '修改前', after: '修改后' })
    expect(auditDiffHeadings('CREATE')).toEqual({ before: '新增前', after: '新增值' })
    expect(auditDiffHeadings('DELETE')).toEqual({ before: '删除前值', after: '删除后' })
  })

  it('groups main table changes separately and summarizes each location', () => {
    const groups = groupAuditFields(fields, true)
    expect(groups.map(group => [group.label, group.total])).toEqual([
      ['主表字段', 3],
      ['子表字段', 2],
    ])
    expect(groups[0].fields.map(field => field.id)).toEqual([
      'main-first',
      'main-late',
      'main-last',
    ])
    expect(auditFieldSummary(fields)).toBe('主表 3 项，子表 2 项')
    expect(auditFieldSummary([], 7)).toBe('7 项变化')
  })

  it('resolves audit value views into display text and kind', () => {
    expect(resolveAuditValueView(null)).toEqual({ text: '—', kind: 'empty' })
    expect(resolveAuditValueView({ state: 'NULL' })).toEqual({ text: '空值', kind: 'null' })
    expect(resolveAuditValueView({ state: 'ABSENT' })).toEqual({ text: '不存在', kind: 'absent' })
    expect(resolveAuditValueView({ omitted: true })).toEqual({ text: '仅记录变更', kind: 'omitted' })
    expect(resolveAuditValueView({ state: 'VALUE', protectedValue: true })).toEqual({ text: '已脱敏', kind: 'masked' })
    expect(resolveAuditValueView({ display: '张三' })).toEqual({ text: '张三', kind: 'value' })
  })

  it('builds compact actor initials', () => {
    expect(actorInitial('张三')).toBe('张')
    expect(actorInitial('')).toBe('系')
  })

  it('hides technical field codes and relation keys from user-facing titles', () => {
    expect(isTechnicalAuditCode('__childRows')).toBe(true)
    expect(isTechnicalAuditCode('detail_ujpc')).toBe(true)
    expect(isTechnicalAuditCode('子表行变更')).toBe(false)
    expect(auditFieldTitle({
      fieldCode: '__childRows',
      fieldType: 'CHILD_SUMMARY',
      relationKey: 'detail_ujpc',
      fieldLabel: '__childRows',
    })).toBe('子表行变更')
    expect(auditFieldTitle({
      fieldCode: 'name',
      fieldLabel: '名称',
    })).toBe('名称')
  })
})
