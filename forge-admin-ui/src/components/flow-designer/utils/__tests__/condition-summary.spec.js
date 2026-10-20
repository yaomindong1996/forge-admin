import { describe, expect, it } from 'vitest'
import { summarizeCondition } from '../condition-summary.js'

describe('可读条件摘要', () => {
  const fields = [{ field: 'amount', label: '报销金额', dataType: 'number' }]
  const condition = '$' + '{amount >= 5000}'
  it('按真实字段目录反解析，格式化数值', () => {
    expect(summarizeCondition({ condition }, fields)).toBe('报销金额 ≥ 5,000')
  })
  it('默认边不显示残留草稿条件', () => {
    expect(summarizeCondition({ condition, isDefault: true }, fields)).toBe('')
  })
  it('无法安全识别或无字段目录时不伪造业务摘要', () => {
    expect(summarizeCondition({ condition })).toBe('')
    expect(summarizeCondition({ condition: '$' + '{custom.check()}' }, fields)).toBe('')
  })

  it('数值分组不经 Number，保留长整数和小数精度', () => {
    const edge = { conditionRules: [{ field: 'amount', operator: 'eq', value: '90071992547409931.40' }] }
    expect(summarizeCondition(edge, fields)).toBe('报销金额 = 90,071,992,547,409,931.40')
  })
})
