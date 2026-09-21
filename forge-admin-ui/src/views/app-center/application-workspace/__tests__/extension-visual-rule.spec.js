import { describe, expect, it } from 'vitest'
import {
  extensionFieldOptions,
  extensionFieldValueKind,
  extensionMatchesPage,
  extensionPageOptions,
  preferredExtensionObjectId,
  resolveExtensionPageContext,
  validateExtensionVisualRule,
} from '../extension-visual-rule'

describe('extension visual rule usability', () => {
  const fields = [
    { fieldCode: 'amount', fieldName: '金额', fieldType: 'MONEY' },
    { fieldCode: 'customerType', fieldName: '客户类型', fieldType: 'DICT', dictType: 'customer_type' },
    { fieldCode: 'readonlyCode', fieldName: '只读编码', readonly: true },
    { fieldCode: 'id', fieldName: '主键', systemField: true },
  ]

  it('automatically selects only a unique object or unique primary object', () => {
    expect(preferredExtensionObjectId([{ objectId: 9 }])).toBe('9')
    expect(preferredExtensionObjectId([{ objectId: 9 }, { objectId: 10, objectRole: 'PRIMARY' }])).toBe('10')
    expect(preferredExtensionObjectId([{ objectId: 9 }, { objectId: 10 }])).toBeNull()
  })

  it('resolves page context for extension defaults and page filters', () => {
    const pages = [
      { id: 'page_leave', title: '请假申请', type: 'page', objectRef: { objectCode: 'leave_form', objectId: 88 } },
    ]
    const entries = [
      { id: 'entry-1', objectCode: 'leave_form', objectId: 88, pageId: 'page_leave' },
    ]
    const objects = [
      { objectId: 88, objectCode: 'leave_form', objectName: '请假' },
    ]

    expect(resolveExtensionPageContext('page_leave', { pages, entries, objects })).toEqual({
      scopeKey: 'page_leave',
      pageTitle: '请假申请',
      objectId: '88',
      entryId: 'entry-1',
    })
    expect(extensionMatchesPage({ scopeKey: 'page_leave' }, 'page_leave')).toBe(true)
    expect(extensionMatchesPage({ objectId: 88 }, 'page_leave', { objectId: '88' })).toBe(true)
    expect(extensionMatchesPage({ objectId: 99 }, 'page_leave', { objectId: '88' })).toBe(false)
  })

  it('keeps invalid legacy values visible and filters non-writable targets', () => {
    expect(extensionFieldOptions(fields, 'removedField')[0]).toMatchObject({ value: 'removedField', invalid: true })
    expect(extensionFieldOptions(fields, '', { writable: true }).map(item => item.value)).toEqual(['amount', 'customerType'])
  })

  it('selects a value editor from field metadata', () => {
    expect(extensionFieldValueKind(fields[0])).toBe('NUMBER')
    expect(extensionFieldValueKind(fields[1])).toBe('DICT')
    expect(extensionFieldValueKind({ fieldType: 'SWITCH' })).toBe('BOOLEAN')
    expect(extensionFieldValueKind({ dataType: 'datetime' })).toBe('DATETIME')
  })

  it('falls back to text when a legacy rule field is missing', () => {
    expect(extensionFieldValueKind(null)).toBe('TEXT')
    expect(extensionFieldValueKind(undefined)).toBe('TEXT')
  })

  it('offers business page names and keeps an invalid legacy page code visible', () => {
    const pages = [
      { id: 'group_presale', type: 'group', title: '售前管理', parentId: null },
      { id: 'page_registration', type: 'page', title: '预售登记', parentId: 'group_presale' },
    ]

    expect(extensionPageOptions(pages)).toEqual([
      { label: '售前管理 / 预售登记', value: 'page_registration' },
    ])
    expect(extensionPageOptions(pages, 'removed_page')[0]).toMatchObject({
      value: 'removed_page',
      invalid: true,
    })
  })

  it('reports row-specific invalid fields and action payloads', () => {
    expect(validateExtensionVisualRule({
      conditions: [{ field: 'removedField', operator: 'EQ', value: '' }],
      actions: [{ actionType: 'SET_FIELD', field: 'readonlyCode', value: '' }],
    }, fields)).toEqual([
      '条件第 1 行字段已失效，请重新选择',
      '条件第 1 行请填写比较值',
      '动作第 1 行目标字段已失效或不可写，请重新选择',
      '动作第 1 行请填写设置值',
    ])
  })

  it('does not throw when a legacy condition row is empty', () => {
    expect(validateExtensionVisualRule({
      conditions: [null],
      actions: [{ actionType: 'SHOW_MESSAGE', message: '提示' }],
    }, fields)).toEqual([
      '条件第 1 行请选择字段',
      '条件第 1 行请选择比较方式',
      '条件第 1 行请填写比较值',
    ])
  })
})
