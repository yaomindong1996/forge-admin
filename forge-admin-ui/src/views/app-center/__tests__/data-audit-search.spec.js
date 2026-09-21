import { describe, expect, it } from 'vitest'
import {
  buildDataAuditApplicationOptions,
  buildDataAuditFieldOptions,
  buildDataAuditPageOptions,
  buildDataAuditSearchParams,
  normalizeDataAuditFilterOptions,
  resolveDataAuditObjectId,
} from '../data-audit-search'

const applications = normalizeDataAuditFilterOptions({
  applications: [{
    applicationId: 1,
    applicationName: '采购管理',
    pages: [{
      pageId: 'purchase-list',
      pageName: '采购申请',
      objectId: '2101126754308390913',
      objectName: '采购申请',
      fields: [
        { fieldCode: 'amount', fieldLabel: '采购金额' },
        { fieldCode: 'status', fieldLabel: '申请状态' },
      ],
    }],
  }],
})

describe('data change audit search', () => {
  it('maps the selected page to its object and strips display-only linkage fields', () => {
    expect(buildDataAuditSearchParams({
      applicationId: '1',
      pageId: 'purchase-list',
      recordKeyword: '采购-2026',
      fieldCode: 'amount',
      actorId: '8',
      actorIdName: '张三',
      timeRange: [100, 200],
    }, applications, value => `T${value}`)).toEqual({
      accessMode: 'AUDIT',
      objectId: '2101126754308390913',
      recordKeyword: '采购-2026',
      fieldCode: 'amount',
      actorId: '8',
      startTime: 'T100',
      endTime: 'T200',
    })
  })

  it('builds business-name choices for each linkage level', () => {
    expect(buildDataAuditApplicationOptions(applications)).toEqual([
      { label: '采购管理', value: '1' },
    ])
    expect(buildDataAuditPageOptions(applications, '1')).toEqual([
      { label: '采购申请', value: 'purchase-list' },
    ])
    expect(buildDataAuditFieldOptions(applications, '1', 'purchase-list')).toEqual([
      { label: '采购金额', value: 'amount' },
      { label: '申请状态', value: 'status' },
    ])
  })

  it('does not submit a field code until a valid page is selected', () => {
    expect(buildDataAuditSearchParams({
      applicationId: '1',
      fieldCode: 'amount',
    }, applications)).toEqual({
      accessMode: 'AUDIT',
    })
  })

  it('keeps snowflake object ids as strings and can reuse the selected object fallback', () => {
    expect(resolveDataAuditObjectId(applications, '1', 'purchase-list')).toBe('2101126754308390913')
    expect(buildDataAuditSearchParams({
      fieldCode: 'fieldNumber',
    }, applications, value => value, '2101126754308390913')).toEqual({
      accessMode: 'AUDIT',
      objectId: '2101126754308390913',
      fieldCode: 'fieldNumber',
    })
  })
})
