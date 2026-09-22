import { describe, expect, it } from 'vitest'
import {
  inferFormFieldsFromColumns,
  PAGE_OBJECT_CREATE_BLANK,
  PAGE_OBJECT_CREATE_DB_IMPORT,
  resolvePageCreateMode,
} from '../page-table-import'

describe('page table import field inference', () => {
  it('skips system and primary key columns then infers business field types', () => {
    const fields = inferFormFieldsFromColumns([
      { columnName: 'id', isPk: 1, columnType: 'bigint', javaField: 'id' },
      { columnName: 'tenant_id', columnType: 'bigint' },
      { columnName: 'create_time', columnType: 'datetime' },
      { columnName: 'customer_name', columnComment: '客户名称', columnType: 'varchar(64)', isRequired: 1, javaField: 'customerName' },
      { columnName: 'amount', columnComment: '金额', columnType: 'decimal(18,2)', javaField: 'amount' },
      { columnName: 'enabled', columnComment: '是否启用', columnType: 'tinyint(1)', javaField: 'enabled', columnDefault: '1' },
      { columnName: 'remark', columnComment: '备注', columnType: 'text', javaField: 'remark' },
      { columnName: 'signed_at', columnComment: '签约时间', columnType: 'datetime', javaField: 'signedAt', columnDefault: 'CURRENT_TIMESTAMP' },
      { columnName: 'status', columnComment: '状态', columnType: 'varchar(16)', dictType: 'customer_status', javaField: 'status', columnDefault: "'ACTIVE'" },
    ])

    expect(fields.map(field => field.fieldCode)).toEqual([
      'customerName',
      'amount',
      'enabled',
      'remark',
      'signedAt',
      'status',
    ])
    expect(fields).toEqual(expect.arrayContaining([
      expect.objectContaining({
        fieldCode: 'customerName',
        fieldType: 'TEXT',
        componentType: 'input',
        required: true,
        fieldBinding: expect.objectContaining({ createIfMissing: false, source: 'db_import' }),
      }),
      expect.objectContaining({
        fieldCode: 'amount',
        fieldType: 'MONEY',
        componentType: 'number',
        dataType: 'decimal',
        precision: 2,
      }),
      expect.objectContaining({
        fieldCode: 'enabled',
        fieldType: 'SWITCH',
        componentType: 'switch',
        defaultValue: 1,
      }),
      expect.objectContaining({
        fieldCode: 'remark',
        fieldType: 'MULTILINE',
        componentType: 'textarea',
      }),
      expect.objectContaining({
        fieldCode: 'signedAt',
        fieldType: 'DATETIME',
        componentType: 'datetime',
        defaultValue: '$forge:now',
      }),
      expect.objectContaining({
        fieldCode: 'status',
        fieldType: 'DICT',
        componentType: 'dictSelect',
        dictType: 'customer_status',
        defaultValue: 'ACTIVE',
      }),
    ]))
  })

  it('maps existing-table selection to db import create mode', () => {
    expect(resolvePageCreateMode({ dataSourceMode: 'CREATE' })).toBe(PAGE_OBJECT_CREATE_BLANK)
    expect(resolvePageCreateMode({ dataSourceMode: 'EXISTING_TABLE' })).toBe(PAGE_OBJECT_CREATE_DB_IMPORT)
  })
})
