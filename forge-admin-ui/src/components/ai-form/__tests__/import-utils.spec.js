import { describe, expect, it } from 'vitest'
import { buildImportPreview, normalizeImportResult } from '../import-utils'

describe('buildImportPreview', () => {
  it('limits rendered rows while retaining the workbook row count', () => {
    const rows = [
      ['用户名', '状态'],
      ...Array.from({ length: 25 }, (_, index) => [`user_${index + 1}`, index % 2 ? '启用' : '停用']),
    ]

    const preview = buildImportPreview(rows, 20)

    expect(preview.headers).toEqual(['用户名', '状态'])
    expect(preview.totalRows).toBe(25)
    expect(preview.rows).toHaveLength(20)
    expect(preview.rows[0]).toMatchObject({ __rowNumber: 2, column_0: 'user_1', column_1: '停用' })
    expect(preview.truncated).toBe(true)
  })

  it('removes fully blank rows and creates fallback header labels', () => {
    const preview = buildImportPreview([
      ['', '姓名'],
      ['', ''],
      ['1001', '张三'],
    ])

    expect(preview.headers).toEqual(['列1', '姓名'])
    expect(preview.totalRows).toBe(1)
    expect(preview.rows[0].column_0).toBe('1001')
  })
})

describe('normalizeImportResult', () => {
  it('normalizes common and dynamic import error field names', () => {
    const result = normalizeImportResult({
      success: false,
      totalRows: 3,
      successRows: 1,
      failedRows: 2,
      summary: '部分数据导入失败',
      errors: [
        { rowNum: 2, columnName: '手机号', rawValue: 'abc', errorMessage: '格式错误', suggestion: '请输入数字' },
        { rowNum: 3, field: 'status', label: '状态', rawValue: '未知', message: '无法识别字典值' },
      ],
    })

    expect(result).toMatchObject({ success: false, totalRows: 3, successRows: 1, failedRows: 2 })
    expect(result.errors[0]).toMatchObject({ rowNum: 2, label: '手机号', message: '格式错误', suggestion: '请输入数字' })
    expect(result.errors[1]).toMatchObject({ rowNum: 3, field: 'status', label: '状态', message: '无法识别字典值' })
  })

  it('derives missing counters from returned data and errors', () => {
    const result = normalizeImportResult({
      successData: [{ id: 1 }, { id: 2 }],
      errors: [{ rowNum: 4, errorMessage: '必填' }],
    })

    expect(result.totalRows).toBe(3)
    expect(result.successRows).toBe(2)
    expect(result.failedRows).toBe(1)
    expect(result.success).toBe(false)
  })
})
